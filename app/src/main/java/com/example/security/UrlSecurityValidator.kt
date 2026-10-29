package com.example.security

import com.example.domain.UrlValidationResult
import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object UrlSecurityValidator {

    private val ALLOWED_FACEBOOK_HOSTS = setOf(
        "facebook.com",
        "www.facebook.com",
        "m.facebook.com",
        "web.facebook.com",
        "mbasic.facebook.com",
        "touch.facebook.com",
        "fb.watch",
        "www.fb.watch",
        "fb.gg",
        "www.fb.gg"
    )

    private val ALLOWED_CDN_SUFFIXES = listOf(
        ".fbcdn.net",
        ".facebook.com",
        ".akamaihd.net"
    )

    private val KNOWN_OTHER_PLATFORMS = mapOf(
        "tiktok.com" to "TikTok",
        "www.tiktok.com" to "TikTok",
        "vm.tiktok.com" to "TikTok",
        "vt.tiktok.com" to "TikTok",
        "youtube.com" to "YouTube",
        "www.youtube.com" to "YouTube",
        "m.youtube.com" to "YouTube",
        "youtu.be" to "YouTube",
        "instagram.com" to "Instagram",
        "www.instagram.com" to "Instagram",
        "twitter.com" to "X (Twitter)",
        "www.twitter.com" to "X (Twitter)",
        "x.com" to "X (Twitter)",
        "www.x.com" to "X (Twitter)",
        "vimeo.com" to "Vimeo",
        "www.vimeo.com" to "Vimeo",
        "dailymotion.com" to "Dailymotion"
    )

    private val BLOCKED_INTERNAL_HOSTS = setOf(
        "localhost",
        "localhost.localdomain",
        "metadata.google.internal",
        "metadata",
        "instance-data",
        "169.254.169.254",
        "0.0.0.0",
        "[::1]",
        "::1",
        "[::]"
    )

    private val TRACKING_QUERY_PARAMS = setOf(
        "fbclid", "__cft__", "__tn__", "extid", "mibextid", "ref", "refsrc", "s"
    )

    fun validateAndNormalizeFacebookUrl(rawInput: String): UrlValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult.Empty
        }

        if (trimmed.length > 2048 || trimmed.any { it.isISOControl() }) {
            return UrlValidationResult.InvalidFormat
        }

        // Check if scheme is missing and user pasted "facebook.com/watch/?v=..." or "fb.watch/..."
        val candidateString = if (!trimmed.contains("://")) {
            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower.startsWith("facebook.com/") ||
                lower.startsWith("www.facebook.com/") ||
                lower.startsWith("m.facebook.com/") ||
                lower.startsWith("web.facebook.com/") ||
                lower.startsWith("fb.watch/") ||
                lower.startsWith("www.fb.watch/")
            ) {
                "https://$trimmed"
            } else {
                trimmed
            }
        } else {
            trimmed
        }

        val uri = try {
            URI(candidateString)
        } catch (e: Exception) {
            return UrlValidationResult.InvalidFormat
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
            ?: return UrlValidationResult.InvalidFormat

        if (scheme != "https" && scheme != "http") {
            return UrlValidationResult.UnsupportedProtocol(scheme)
        }

        if (!uri.userInfo.isNullOrEmpty()) {
            return UrlValidationResult.SsrfBlocked("Embedded credentials in URL are prohibited")
        }

        val host = uri.host?.lowercase(Locale.ROOT)?.removeSuffix(".")
            ?: return UrlValidationResult.InvalidFormat

        // SSRF check on host and IP patterns
        val ssrfReason = checkHostForSsrf(host)
        if (ssrfReason != null) {
            return UrlValidationResult.SsrfBlocked(ssrfReason)
        }

        // Check known unsupported platforms (TikTok, YouTube, Instagram, etc.)
        KNOWN_OTHER_PLATFORMS[host]?.let { platform ->
            return UrlValidationResult.UnsupportedPlatform(platform, host)
        }
        for ((domain, platform) in KNOWN_OTHER_PLATFORMS) {
            if (host.endsWith(".$domain")) {
                return UrlValidationResult.UnsupportedPlatform(platform, host)
            }
        }

        if (host !in ALLOWED_FACEBOOK_HOSTS) {
            return UrlValidationResult.UnsupportedPlatform("External Domain", host)
        }

        val rawPath = uri.rawPath ?: "/"
        val query = uri.rawQuery ?: ""

        // Validate Facebook video path patterns
        val videoIdHint = extractFacebookVideoHint(host, rawPath, query)
            ?: return UrlValidationResult.UnsupportedFacebookUrl(host, rawPath)

        // Normalize to clean HTTPS URL without tracking parameters
        val normalizedHost = when (host) {
            "fb.watch", "www.fb.watch" -> "fb.watch"
            else -> "www.facebook.com"
        }

        val cleanQuery = filterQueryParameters(query)
        val normalizedPath = if (rawPath.isEmpty()) "/" else rawPath
        val normalizedUrl = buildString {
            append("https://")
            append(normalizedHost)
            append(normalizedPath)
            if (cleanQuery.isNotEmpty()) {
                append("?")
                append(cleanQuery)
            }
        }

        return UrlValidationResult.Valid(
            normalizedUrl = normalizedUrl,
            videoIdHint = videoIdHint,
            host = normalizedHost
        )
    }

    fun isSafeRedirectOrStreamUrl(url: String, allowVerifiedTestHost: Boolean = false): Boolean {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "https" && scheme != "http") return false
        if (!uri.userInfo.isNullOrEmpty()) return false

        val host = uri.host?.lowercase(Locale.ROOT)?.removeSuffix(".") ?: return false
        if (checkHostForSsrf(host) != null) return false

        if (host in ALLOWED_FACEBOOK_HOSTS) return true
        if (ALLOWED_CDN_SUFFIXES.any { host.endsWith(it) }) return true
        if (allowVerifiedTestHost && (host == "commondatastorage.googleapis.com" || host == "storage.googleapis.com")) {
            return true
        }
        return false
    }

    fun checkHostForSsrf(host: String): String? {
        val lower = host.lowercase(Locale.ROOT).trim()
        if (lower in BLOCKED_INTERNAL_HOSTS) {
            return "Blocked internal or loopback host: $lower"
        }
        if (lower.endsWith(".local") || lower.endsWith(".internal") || lower.endsWith(".localhost") || lower.endsWith(".lan")) {
            return "Blocked internal local network TLD: $lower"
        }

        // Check hexadecimal, octal, or integer-encoded IP attempts (e.g., 2130706433 or 0x7f000001)
        if (lower.matches(Regex("^0x[0-9a-f]+$")) || lower.matches(Regex("^\\d+$"))) {
            return "Blocked raw integer or hex IP representation"
        }

        // Check standard IPv4 literal
        val ipv4Regex = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")
        val match = ipv4Regex.matchEntire(lower)
        if (match != null) {
            val octets = match.groupValues.drop(1).mapNotNull { it.toIntOrNull() }
            if (octets.size != 4 || octets.any { it !in 0..255 }) {
                return "Invalid IPv4 address"
            }
            val a = octets[0]
            val b = octets[1]
            if (a == 0 || a == 10 || a == 127) {
                return "Blocked private or loopback IPv4 range ($lower)"
            }
            if (a == 169 && b == 254) {
                return "Blocked link-local / cloud metadata IPv4 ($lower)"
            }
            if (a == 172 && b in 16..31) {
                return "Blocked RFC1918 private network ($lower)"
            }
            if (a == 192 && b == 168) {
                return "Blocked RFC1918 private network ($lower)"
            }
            if (a == 100 && b in 64..127) {
                return "Blocked carrier-grade NAT network ($lower)"
            }
            if (a >= 224) {
                return "Blocked multicast or reserved IPv4 ($lower)"
            }
            return "Direct IP literals are not permitted for video extraction"
        }

        // Check IPv6 literal
        if (lower.startsWith("[") || lower.contains(":")) {
            return "Direct IPv6 literals are not permitted"
        }

        return null
    }

    fun validateResolvedAddresses(host: String, addresses: List<InetAddress>): String? {
        for (addr in addresses) {
            if (addr.isLoopbackAddress ||
                addr.isSiteLocalAddress ||
                addr.isLinkLocalAddress ||
                addr.isAnyLocalAddress ||
                addr.isMulticastAddress
            ) {
                return "DNS resolution for $host pointed to restricted internal address ${addr.hostAddress}"
            }
            val raw = addr.address
            if (raw.size == 4) {
                val b0 = raw[0].toInt() and 0xFF
                val b1 = raw[1].toInt() and 0xFF
                if (b0 == 10 || b0 == 127 || b0 == 0 ||
                    (b0 == 169 && b1 == 254) ||
                    (b0 == 172 && b1 in 16..31) ||
                    (b0 == 192 && b1 == 168) ||
                    (b0 == 100 && b1 in 64..127)
                ) {
                    return "DNS resolution for $host resolved to private/internal IPv4"
                }
            }
        }
        return null
    }

    private fun extractFacebookVideoHint(host: String, path: String, query: String): String? {
        val cleanPath = path.trim('/')
        if (host == "fb.watch" || host == "www.fb.watch") {
            return if (cleanPath.isNotEmpty() && cleanPath.length <= 64) {
                cleanPath.substringBefore('/')
            } else {
                null
            }
        }

        val queryMap = parseQueryMap(query)

        // 1. /watch/?v=12345 or /watch?v=12345
        if (cleanPath == "watch" || cleanPath.startsWith("watch/")) {
            val vParam = queryMap["v"]
            if (!vParam.isNullOrBlank()) return sanitizeId(vParam)
            val afterWatch = cleanPath.removePrefix("watch").trim('/')
            if (afterWatch.isNotEmpty()) return sanitizeId(afterWatch)
        }

        // 2. /video.php?v=12345 or /story.php?story_fbid=... or /permalink.php?story_fbid=...
        if (cleanPath == "video.php" || cleanPath == "story.php" || cleanPath == "permalink.php") {
            val id = queryMap["v"] ?: queryMap["story_fbid"] ?: queryMap["id"]
            if (!id.isNullOrBlank()) return sanitizeId(id)
        }

        // 3. /share/v/XXXX or /share/r/XXXX or /share/XXXX
        val segments = cleanPath.split('/').filter { it.isNotBlank() }
        if (segments.size >= 2 && segments[0] == "share") {
            val candidate = if (segments.size >= 3 && (segments[1] == "v" || segments[1] == "r")) {
                segments[2]
            } else {
                segments[1]
            }
            if (candidate.isNotBlank()) return sanitizeId(candidate)
        }

        // 4. /reel/12345 or /reels/12345
        if (segments.size >= 2 && (segments[0] == "reel" || segments[0] == "reels")) {
            return sanitizeId(segments[1])
        }

        // 5. /<page_or_user>/videos/<video_id> or /<page_or_user>/videos/<slug>/<video_id>
        val videosIndex = segments.indexOf("videos")
        if (videosIndex != -1 && videosIndex + 1 < segments.size) {
            val lastSeg = segments.last()
            val nextSeg = segments[videosIndex + 1]
            return sanitizeId(if (lastSeg.all { it.isDigit() }) lastSeg else nextSeg)
        }

        // 6. /<page_or_user>/posts/<post_id>
        val postsIndex = segments.indexOf("posts")
        if (postsIndex != -1 && postsIndex + 1 < segments.size) {
            return sanitizeId(segments[postsIndex + 1])
        }

        return null
    }

    private fun parseQueryMap(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) null
            else {
                val k = runCatching { URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()) }.getOrDefault("")
                val v = runCatching { URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) }.getOrDefault("")
                if (k.isNotBlank()) k to v else null
            }
        }.toMap()
    }

    private fun filterQueryParameters(query: String): String {
        if (query.isBlank()) return ""
        return query.split('&').filter { pair ->
            val key = pair.substringBefore('=').lowercase(Locale.ROOT)
            key.isNotBlank() &&
                key !in TRACKING_QUERY_PARAMS &&
                !key.startsWith("__cft__") &&
                !key.startsWith("utm_")
        }.joinToString("&")
    }

    private fun sanitizeId(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_-]"), "").take(48).ifEmpty { "fb_video" }
    }

    fun sanitizeMetadataText(raw: String?, maxLength: Int = 180): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw
            .replace(Regex("<[^>]*>"), "") // Strip HTML tags
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (cleaned.isEmpty()) null else cleaned.take(maxLength)
    }

    fun sanitizeSafeFilename(title: String?, videoId: String, resolution: String): String {
        val base = (title ?: "facebook_video_$videoId")
            .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF\\u00C0-\\u017F _-]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(48)
            .ifEmpty { "velostream_$videoId" }
        val safeRes = resolution.replace(Regex("[^a-zA-Z0-9]"), "").take(10).ifEmpty { "mp4" }
        return "${base}_${safeRes}.mp4"
    }
}

class SlidingWindowRateLimiter(
    private val maxRequests: Int = 8,
    private val windowMillis: Long = 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val timestamps = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(): RateLimitDecision {
        val now = clock()
        while (timestamps.isNotEmpty() && now - timestamps.first() >= windowMillis) {
            timestamps.removeFirst()
        }
        if (timestamps.size >= maxRequests) {
            val oldest = timestamps.first()
            val retryAfterSec = (((windowMillis - (now - oldest)) + 999L) / 1000L).toInt().coerceAtLeast(1)
            return RateLimitDecision(allowed = false, remaining = 0, retryAfterSeconds = retryAfterSec)
        }
        timestamps.addLast(now)
        return RateLimitDecision(
            allowed = true,
            remaining = (maxRequests - timestamps.size).coerceAtLeast(0),
            retryAfterSeconds = 0
        )
    }

    @Synchronized
    fun remainingQuota(): Int {
        val now = clock()
        while (timestamps.isNotEmpty() && now - timestamps.first() >= windowMillis) {
            timestamps.removeFirst()
        }
        return (maxRequests - timestamps.size).coerceAtLeast(0)
    }

    @Synchronized
    fun reset() {
        timestamps.clear()
    }
}

data class RateLimitDecision(
    val allowed: Boolean,
    val remaining: Int,
    val retryAfterSeconds: Int
)
