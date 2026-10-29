package com.example

import com.example.domain.AppLanguage
import com.example.domain.UrlValidationResult
import com.example.extractor.FacebookVideoProvider
import com.example.extractor.ProviderRegistry
import com.example.extractor.TikTokVideoProviderStub
import com.example.i18n.AppStringsRepository
import com.example.security.SlidingWindowRateLimiter
import com.example.security.UrlSecurityValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `empty url returns Empty validation result`() {
        assertEquals(UrlValidationResult.Empty, UrlSecurityValidator.validateAndNormalizeFacebookUrl(""))
        assertEquals(UrlValidationResult.Empty, UrlSecurityValidator.validateAndNormalizeFacebookUrl("   "))
    }

    @Test
    fun `invalid url syntax returns InvalidFormat`() {
        assertEquals(UrlValidationResult.InvalidFormat, UrlSecurityValidator.validateAndNormalizeFacebookUrl("not a url"))
        assertEquals(UrlValidationResult.InvalidFormat, UrlSecurityValidator.validateAndNormalizeFacebookUrl("://missing-scheme"))
    }

    @Test
    fun `unsupported protocols are blocked`() {
        val jsResult = UrlSecurityValidator.validateAndNormalizeFacebookUrl("javascript:alert(1)")
        assertTrue(jsResult is UrlValidationResult.UnsupportedProtocol)

        val fileResult = UrlSecurityValidator.validateAndNormalizeFacebookUrl("file:///etc/passwd")
        assertTrue(fileResult is UrlValidationResult.UnsupportedProtocol)

        val ftpResult = UrlSecurityValidator.validateAndNormalizeFacebookUrl("ftp://www.facebook.com/watch?v=123")
        assertTrue(ftpResult is UrlValidationResult.UnsupportedProtocol)
    }

    @Test
    fun `ssrf protection blocks localhost, private subnets, metadata ip, and credentials`() {
        val maliciousUrls = listOf(
            "http://localhost:8080/watch?v=123",
            "http://127.0.0.1/watch?v=123",
            "http://10.0.0.1/watch?v=123",
            "http://172.16.5.4/watch?v=123",
            "http://192.168.1.100/watch?v=123",
            "http://169.254.169.254/latest/meta-data/",
            "http://0.0.0.0/watch?v=123",
            "http://[::1]/watch?v=123",
            "http://metadata.google.internal/computeMetadata/v1/",
            "https://admin:secret@www.facebook.com/watch/?v=12345"
        )
        for (url in maliciousUrls) {
            val result = UrlSecurityValidator.validateAndNormalizeFacebookUrl(url)
            assertTrue("Expected SSRF block for $url but got $result", result is UrlValidationResult.SsrfBlocked)
        }
    }

    @Test
    fun `unsupported platforms like TikTok, YouTube, Instagram are rejected cleanly`() {
        val tiktok = UrlSecurityValidator.validateAndNormalizeFacebookUrl("https://www.tiktok.com/@user/video/123456")
        assertTrue(tiktok is UrlValidationResult.UnsupportedPlatform)
        assertEquals("TikTok", (tiktok as UrlValidationResult.UnsupportedPlatform).platformName)

        val youtube = UrlSecurityValidator.validateAndNormalizeFacebookUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(youtube is UrlValidationResult.UnsupportedPlatform)
        assertEquals("YouTube", (youtube as UrlValidationResult.UnsupportedPlatform).platformName)

        val instagram = UrlSecurityValidator.validateAndNormalizeFacebookUrl("https://www.instagram.com/reel/abc123/")
        assertTrue(instagram is UrlValidationResult.UnsupportedPlatform)
        assertEquals("Instagram", (instagram as UrlValidationResult.UnsupportedPlatform).platformName)
    }

    @Test
    fun `valid Facebook video URL formats are normalized and stripped of tracking params`() {
        val watchUrl = UrlSecurityValidator.validateAndNormalizeFacebookUrl(
            "facebook.com/watch/?v=10153231379946729&fbclid=IwAR12345&utm_source=share"
        )
        assertTrue(watchUrl is UrlValidationResult.Valid)
        val validWatch = watchUrl as UrlValidationResult.Valid
        assertEquals("https://www.facebook.com/watch/?v=10153231379946729", validWatch.normalizedUrl)
        assertEquals("10153231379946729", validWatch.videoIdHint)

        val fbWatch = UrlSecurityValidator.validateAndNormalizeFacebookUrl("https://fb.watch/v1d3oId99/?mibextid=abc")
        assertTrue(fbWatch is UrlValidationResult.Valid)
        assertEquals("https://fb.watch/v1d3oId99/", (fbWatch as UrlValidationResult.Valid).normalizedUrl)

        val shareV = UrlSecurityValidator.validateAndNormalizeFacebookUrl("https://www.facebook.com/share/v/15aBcDeFgH/")
        assertTrue(shareV is UrlValidationResult.Valid)
        assertEquals("15aBcDeFgH", (shareV as UrlValidationResult.Valid).videoIdHint)

        val reelUrl = UrlSecurityValidator.validateAndNormalizeFacebookUrl("https://m.facebook.com/reel/849201746291048")
        assertTrue(reelUrl is UrlValidationResult.Valid)
        assertEquals("https://www.facebook.com/reel/849201746291048", (reelUrl as UrlValidationResult.Valid).normalizedUrl)

        val permalinkUrl = UrlSecurityValidator.validateAndNormalizeFacebookUrl("https://www.facebook.com/Meta/videos/9876543210/")
        assertTrue(permalinkUrl is UrlValidationResult.Valid)
        assertEquals("9876543210", (permalinkUrl as UrlValidationResult.Valid).videoIdHint)
    }

    @Test
    fun `real Facebook Relay HTML payload parser extracts HD, SD, title, author, thumbnail, and duration`() {
        val provider = FacebookVideoProvider()
        val sampleRelayHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="Engineering Deep Dive: High Speed Video Streaming &amp; Media Delivery" />
                <meta property="og:image" content="https://scontent.xx.fbcdn.net/v/t15.5256-10/thumb_123.jpg?stp=dst-jpg" />
            </head>
            <body>
                <script type="application/json">
                {
                    "owner": {"name": "Meta Engineering"},
                    "playable_duration_in_ms": 142000,
                    "browser_native_hd_url": "https:\/\/video.xx.fbcdn.net\/v\/t42.1790-2\/1080p_stream.mp4?_nc_cat=1\u0026efg=eyJ2ZW5jb2RlX3RhZyI6ImhkIn0=",
                    "browser_native_sd_url": "https:\/\/video.xx.fbcdn.net\/v\/t42.1790-2\/360p_stream.mp4?_nc_cat=1\u0026efg=eyJ2ZW5jb2RlX3RhZyI6InNkIn0="
                }
                </script>
            </body>
            </html>
        """.trimIndent()

        val parsed = provider.parseFacebookHtmlPayload(sampleRelayHtml)
        assertEquals("Engineering Deep Dive: High Speed Video Streaming & Media Delivery", parsed.title)
        assertEquals("Meta Engineering", parsed.author)
        assertEquals(142L, parsed.durationSeconds)
        assertEquals("https://scontent.xx.fbcdn.net/v/t15.5256-10/thumb_123.jpg?stp=dst-jpg", parsed.thumbnailUrl)
        assertEquals(2, parsed.qualities.size)
        assertTrue(parsed.qualities[0].isHd)
        assertEquals(
            "https://video.xx.fbcdn.net/v/t42.1790-2/1080p_stream.mp4?_nc_cat=1&efg=eyJ2ZW5jb2RlX3RhZyI6ImhkIn0=",
            parsed.qualities[0].streamUrl
        )
        assertFalse(parsed.qualities[1].isHd)
    }

    @Test
    fun `sliding window rate limiter enforces request quota accurately`() {
        var fakeTime = 1_000_000L
        val limiter = SlidingWindowRateLimiter(maxRequests = 3, windowMillis = 60_000L, clock = { fakeTime })

        assertTrue(limiter.tryAcquire().allowed)
        assertTrue(limiter.tryAcquire().allowed)
        assertTrue(limiter.tryAcquire().allowed)

        val fourth = limiter.tryAcquire()
        assertFalse(fourth.allowed)
        assertEquals(60, fourth.retryAfterSeconds)

        fakeTime += 61_000L
        assertTrue(limiter.tryAcquire().allowed)
    }

    @Test
    fun `trilingual localization provides complete EN, AR RTL, and FR LTR strings`() {
        val en = AppStringsRepository.forLanguage(AppLanguage.EN)
        val ar = AppStringsRepository.forLanguage(AppLanguage.AR)
        val fr = AppStringsRepository.forLanguage(AppLanguage.FR)

        assertFalse(en.language.isRtl)
        assertTrue(ar.language.isRtl)
        assertFalse(fr.language.isRtl)

        assertEquals(6, en.faqs.size)
        assertEquals(6, ar.faqs.size)
        assertEquals(6, fr.faqs.size)

        assertEquals(8, en.features.size)
        assertEquals(8, ar.features.size)
        assertEquals(8, fr.features.size)
    }

    @Test
    fun `modular provider registry enables Facebook in v1 and isolates TikTok stub`() {
        val registry = ProviderRegistry(listOf(FacebookVideoProvider(), TikTokVideoProviderStub()))
        val fb = registry.findProviderForHost("www.facebook.com")
        val tt = registry.findProviderForHost("www.tiktok.com")
        assertNotNull(fb)
        assertTrue(fb!!.isEnabledInCurrentVersion)
        assertNotNull(tt)
        assertFalse(tt!!.isEnabledInCurrentVersion)
        assertNull(registry.findProviderForHost("example.com"))
    }
}
