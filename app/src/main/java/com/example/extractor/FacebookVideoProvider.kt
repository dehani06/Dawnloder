package com.example.extractor

import com.example.domain.ExtractionErrorType
import com.example.domain.VideoMetadata
import com.example.domain.VideoQualityOption
import com.example.security.UrlSecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class FacebookVideoProvider(
    private val httpClient: OkHttpClient = createDefaultSecureHttpClient()
) : VideoProvider {

    override val providerId: String = "facebook"
    override val displayName: String = "Facebook"
    override val isEnabledInCurrentVersion: Boolean = true

    override fun supportsHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "facebook.com" ||
            lower.endsWith(".facebook.com") ||
            lower == "fb.watch" ||
            lower == "www.fb.watch" ||
            lower == "fb.gg"
    }

    override suspend fun extractVideoMetadata(
        normalizedUrl: String,
        videoIdHint: String?
    ): ExtractionOutcome = withContext(Dispatchers.IO) {
        try {
            val candidates = buildCandidateEndpoints(normalizedUrl)
            var lastHttpCode: Int? = null
            var sawLoginWall = false
            var bestTitle: String? = null
            var bestAuthor: String? = null
            var bestThumbnail: String? = null
            var bestDuration: Long? = null
            val discoveredStreams = linkedMapOf<String, VideoQualityOption>()
            var winningStrategy = ""

            for ((strategyName, endpointUrl, userAgent) in candidates) {
                val htmlResult = fetchHtmlSafely(endpointUrl, userAgent)
                if (htmlResult.httpCode != null) {
                    lastHttpCode = htmlResult.httpCode
                }
                val body = htmlResult.body ?: continue

                if (isFacebookLoginOrRestrictedPage(body)) {
                    sawLoginWall = true
                }

                val parsed = parseFacebookHtmlPayload(body)
                if (bestTitle == null && parsed.title != null) bestTitle = parsed.title
                if (bestAuthor == null && parsed.author != null) bestAuthor = parsed.author
                if (bestThumbnail == null && parsed.thumbnailUrl != null) bestThumbnail = parsed.thumbnailUrl
                if (bestDuration == null && parsed.durationSeconds != null) bestDuration = parsed.durationSeconds

                for (stream in parsed.qualities) {
                    if (UrlSecurityValidator.isSafeRedirectOrStreamUrl(stream.streamUrl)) {
                        discoveredStreams.putIfAbsent(stream.id, stream)
                    }
                }

                if (discoveredStreams.isNotEmpty() && winningStrategy.isEmpty()) {
                    winningStrategy = strategyName
                    // If we already have both HD and SD, no need to query additional endpoints
                    if (discoveredStreams.values.any { it.isHd } && discoveredStreams.size >= 2) {
                        break
                    }
                }
            }

            if (discoveredStreams.isEmpty()) {
                val errorType = when {
                    sawLoginWall -> ExtractionErrorType.VIDEO_UNAVAILABLE_OR_PRIVATE
                    lastHttpCode != null && lastHttpCode >= 400 -> ExtractionErrorType.HTTP_ERROR
                    else -> ExtractionErrorType.NO_STREAMS_FOUND
                }
                val detail = when {
                    sawLoginWall ->
                        "Facebook returned an authentication or age/privacy gate for this video. Only public videos accessible without credentials can be extracted."
                    lastHttpCode != null && lastHttpCode >= 400 ->
                        "Facebook returned HTTP $lastHttpCode while requesting video metadata."
                    else ->
                        "No direct progressive MP4 or DASH video streams were exposed in Facebook's public response for this URL."
                }
                return@withContext ExtractionOutcome.Failure(
                    errorType = errorType,
                    technicalDetail = detail,
                    httpStatusCode = lastHttpCode
                )
            }

            // Probe Content-Length for discovered streams so file sizes are genuinely reported
            val enrichedQualities = discoveredStreams.values.map { option ->
                val probedSize = probeStreamSizeBytes(option.streamUrl)
                if (probedSize != null && probedSize > 0L) {
                    option.copy(fileSizeBytes = probedSize)
                } else {
                    option
                }
            }.sortedByDescending { it.isHd }

            val resolvedId = videoIdHint ?: "fb_${normalizedUrl.hashCode().toUInt()}"
            ExtractionOutcome.Success(
                VideoMetadata(
                    videoId = resolvedId,
                    canonicalUrl = normalizedUrl,
                    providerId = providerId,
                    title = bestTitle,
                    authorName = bestAuthor,
                    durationSeconds = bestDuration,
                    thumbnailUrl = bestThumbnail?.takeIf { UrlSecurityValidator.isSafeRedirectOrStreamUrl(it) },
                    qualities = enrichedQualities,
                    extractionStrategy = winningStrategy.ifEmpty { "Relay JSON + OpenGraph Parser" }
                )
            )
        } catch (e: SocketTimeoutException) {
            ExtractionOutcome.Failure(
                errorType = ExtractionErrorType.TIMEOUT,
                technicalDetail = "Timed out waiting for Facebook server response (${e.message ?: "read timeout"})."
            )
        } catch (e: SecurityException) {
            ExtractionOutcome.Failure(
                errorType = ExtractionErrorType.HTTP_ERROR,
                technicalDetail = e.message ?: "Blocked unsafe redirect target."
            )
        } catch (e: IOException) {
            ExtractionOutcome.Failure(
                errorType = ExtractionErrorType.NETWORK_ERROR,
                technicalDetail = "Network communication error: ${e.message ?: e.javaClass.simpleName}"
            )
        } catch (e: Exception) {
            ExtractionOutcome.Failure(
                errorType = ExtractionErrorType.NO_STREAMS_FOUND,
                technicalDetail = "Unexpected extraction error: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Parses actual Facebook HTML / Relay JSON payloads for progressive HD/SD URLs,
     * DASH video representations, OpenGraph video tags, title, uploader, thumbnail, and duration.
     * Exposed as internal/public for deterministic unit testing against real Facebook payload structures.
     */
    fun parseFacebookHtmlPayload(html: String): ParsedFacebookPayload {
        val qualities = mutableListOf<VideoQualityOption>()
        val seenUrls = mutableSetOf<String>()

        fun addStream(id: String, label: String, resolution: String, rawUrl: String, isHd: Boolean, hasAudio: Boolean = true) {
            val decoded = decodeFacebookEscapedUrl(rawUrl)
            if (decoded.startsWith("https://") || decoded.startsWith("http://")) {
                if (seenUrls.add(decoded)) {
                    qualities.add(
                        VideoQualityOption(
                            id = id,
                            label = label,
                            resolution = resolution,
                            format = "MP4",
                            streamUrl = decoded,
                            isHd = isHd,
                            hasAudio = hasAudio,
                            mimeType = "video/mp4"
                        )
                    )
                }
            }
        }

        // 1. Relay / Tahoe HD progressive patterns
        val hdPatterns = listOf(
            Regex(""""browser_native_hd_url"\s*:\s*"([^"]+)""""),
            Regex(""""playable_url_quality_hd"\s*:\s*"([^"]+)""""),
            Regex(""""hd_src_no_ratelimit"\s*:\s*"([^"]+)""""),
            Regex(""""hd_src"\s*:\s*"([^"]+)"""")
        )
        for (regex in hdPatterns) {
            regex.findAll(html).forEach { match ->
                val raw = match.groupValues[1]
                if (raw != "null" && raw.isNotBlank()) {
                    addStream(
                        id = "hd_${qualities.size + 1}",
                        label = "HD 720p / 1080p",
                        resolution = "720p+",
                        rawUrl = raw,
                        isHd = true
                    )
                }
            }
        }

        // 2. Relay / Tahoe SD progressive patterns
        val sdPatterns = listOf(
            Regex(""""browser_native_sd_url"\s*:\s*"([^"]+)""""),
            Regex(""""playable_url"\s*:\s*"([^"]+)""""),
            Regex(""""sd_src_no_ratelimit"\s*:\s*"([^"]+)""""),
            Regex(""""sd_src"\s*:\s*"([^"]+)"""")
        )
        for (regex in sdPatterns) {
            regex.findAll(html).forEach { match ->
                val raw = match.groupValues[1]
                if (raw != "null" && raw.isNotBlank()) {
                    addStream(
                        id = "sd_${qualities.size + 1}",
                        label = "Standard 360p / 480p",
                        resolution = "360p",
                        rawUrl = raw,
                        isHd = false
                    )
                }
            }
        }

        // 3. OpenGraph direct video tags (<meta property="og:video:secure_url" content="...">)
        val ogVideoRegex = Regex("""<meta\s+property=["']og:video(?::secure_url|:url)?["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        ogVideoRegex.findAll(html).forEach { match ->
            val raw = match.groupValues[1]
            if (raw.contains(".mp4") || raw.contains("fbcdn.net")) {
                addStream(
                    id = "og_${qualities.size + 1}",
                    label = "Original Stream (OpenGraph)",
                    resolution = "Auto",
                    rawUrl = raw,
                    isHd = false
                )
            }
        }

        // 4. Mobile mbasic / direct redirect video links (/video_redirect/?src=...)
        val redirectSrcRegex = Regex("""/video_redirect/\?src=([^"&'\\]+)""")
        redirectSrcRegex.findAll(html).forEach { match ->
            val encodedSrc = match.groupValues[1]
            val decodedSrc = runCatching {
                java.net.URLDecoder.decode(decodeFacebookEscapedUrl(encodedSrc), StandardCharsets.UTF_8.name())
            }.getOrNull()
            if (!decodedSrc.isNullOrBlank()) {
                addStream(
                    id = "mobile_${qualities.size + 1}",
                    label = "Mobile Direct MP4",
                    resolution = "480p",
                    rawUrl = decodedSrc,
                    isHd = false
                )
            }
        }

        // Extract real title if present
        val rawTitle = extractFirstGroup(
            html,
            listOf(
                Regex("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta\s+name=["']twitter:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex(""""title"\s*:\s*\{\s*"text"\s*:\s*"([^"]+)""""),
                Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
            )
        )?.let { decodeUnicodeEscapes(it) }

        val cleanTitle = UrlSecurityValidator.sanitizeMetadataText(rawTitle)
            ?.takeIf { !it.equals("Facebook", ignoreCase = true) && !it.equals("Log in or sign up to view", ignoreCase = true) }

        // Extract author / page name if present
        val rawAuthor = extractFirstGroup(
            html,
            listOf(
                Regex(""""owner"\s*:\s*\{[^}]*"name"\s*:\s*"([^"]+)""""),
                Regex(""""actors"\s*:\s*\[\s*\{\s*[^}]*"name"\s*:\s*"([^"]+)"""")
            )
        )?.let { UrlSecurityValidator.sanitizeMetadataText(decodeUnicodeEscapes(it), 80) }

        // Extract thumbnail URL if present
        val rawThumb = extractFirstGroup(
            html,
            listOf(
                Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex(""""preferred_thumbnail"\s*:\s*\{\s*"image"\s*:\s*\{\s*"uri"\s*:\s*"([^"]+)""""),
                Regex(""""thumbnailImage"\s*:\s*\{\s*"uri"\s*:\s*"([^"]+)"""")
            )
        )?.let { decodeFacebookEscapedUrl(it) }

        // Extract duration in seconds if present
        val durationMs = extractFirstGroup(
            html,
            listOf(
                Regex(""""playable_duration_in_ms"\s*:\s*(\d+)"""),
                Regex(""""duration_in_ms"\s*:\s*(\d+)""")
            )
        )?.toLongOrNull()

        val durationSec = durationMs?.let { (it / 1000L).coerceAtLeast(1L) }
            ?: extractFirstGroup(
                html,
                listOf(Regex(""""length_in_second"\s*:\s*(\d+)"""))
            )?.toLongOrNull()

        return ParsedFacebookPayload(
            title = cleanTitle,
            author = rawAuthor,
            thumbnailUrl = rawThumb,
            durationSeconds = durationSec,
            qualities = qualities
        )
    }

    private fun isFacebookLoginOrRestrictedPage(html: String): Boolean {
        val lower = html.lowercase()
        val hasLoginForm = lower.contains("id=\"login_form\"") ||
            lower.contains("name=\"login\"") ||
            lower.contains("/login/device-based/") ||
            lower.contains("you must log in to continue")
        val hasVideoUrls = lower.contains("browser_native_") ||
            lower.contains("playable_url") ||
            lower.contains("hd_src") ||
            lower.contains("sd_src")
        return hasLoginForm && !hasVideoUrls
    }

    private fun buildCandidateEndpoints(normalizedUrl: String): List<Triple<String, String, String>> {
        val encodedHref = URLEncoder.encode(normalizedUrl, StandardCharsets.UTF_8.name())
        val embedUrl = "https://www.facebook.com/plugins/video.php?href=$encodedHref&show_text=true&width=734"
        val mbasicUrl = normalizedUrl
            .replace("https://www.facebook.com/", "https://mbasic.facebook.com/")

        return listOf(
            Triple("Facebook Desktop Relay Parser", normalizedUrl, DESKTOP_USER_AGENT),
            Triple("Facebook Video Embed Plugin", embedUrl, DESKTOP_USER_AGENT),
            Triple("Facebook Mobile Basic Parser", mbasicUrl, MOBILE_USER_AGENT)
        )
    }

    private data class HtmlFetchResult(val body: String?, val httpCode: Int?)

    private fun fetchHtmlSafely(url: String, userAgent: String): HtmlFetchResult {
        var currentUrl = url
        var redirects = 0

        while (redirects <= MAX_REDIRECTS) {
            if (!UrlSecurityValidator.isSafeRedirectOrStreamUrl(currentUrl)) {
                throw SecurityException("Blocked redirect to untrusted or internal host: $currentUrl")
            }

            val request = Request.Builder()
                .url(currentUrl)
                .get()
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = response.header("Location") ?: return HtmlFetchResult(null, code)
                    val resolved = response.request.url.resolve(location)?.toString()
                        ?: return HtmlFetchResult(null, code)
                    currentUrl = resolved
                    redirects++
                } else {
                    val bodyStr = response.body?.string()
                    return HtmlFetchResult(bodyStr, code)
                }
            }
        }
        return HtmlFetchResult(null, 310)
    }

    private fun probeStreamSizeBytes(streamUrl: String): Long? {
        if (!UrlSecurityValidator.isSafeRedirectOrStreamUrl(streamUrl)) return null
        return try {
            val headRequest = Request.Builder()
                .url(streamUrl)
                .head()
                .header("User-Agent", DESKTOP_USER_AGENT)
                .build()
            httpClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    response.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decodeFacebookEscapedUrl(raw: String): String {
        return decodeUnicodeEscapes(raw)
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .trim()
    }

    private fun decodeUnicodeEscapes(input: String): String {
        val unicodeRegex = Regex("""\\u([0-9a-fA-F]{4})""")
        return unicodeRegex.replace(input) { match ->
            val charCode = match.groupValues[1].toInt(16)
            charCode.toChar().toString()
        }
    }

    private fun extractFirstGroup(text: String, regexes: List<Regex>): String? {
        for (regex in regexes) {
            val match = regex.find(text)
            if (match != null && match.groupValues.size > 1) {
                val value = match.groupValues[1].trim()
                if (value.isNotEmpty()) return value
            }
        }
        return null
    }

    data class ParsedFacebookPayload(
        val title: String?,
        val author: String?,
        val thumbnailUrl: String?,
        val durationSeconds: Long?,
        val qualities: List<VideoQualityOption>
    )

    companion object {
        private const val MAX_REDIRECTS = 5
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        fun createDefaultSecureHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .followRedirects(false) // Handled manually to validate every redirect hop against SSRF
                .followSslRedirects(false)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }
}
