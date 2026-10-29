# VeloStream — Facebook Video Extractor & MP4 Downloader (v1.0)

VeloStream is a production-oriented Facebook video metadata extractor and binary MP4 streaming downloader built with Kotlin, Jetpack Compose (Material 3), and OkHttp.

---

## 1. Architecture & Provider Scope

- **Initial Scope (v1.0)**: Built exclusively for publicly accessible Facebook video URLs (`facebook.com/watch/?v=...`, `www.facebook.com/watch/?v=...`, `fb.watch/...`, `www.facebook.com/share/v/...`, `facebook.com/reel/...`, and `/<page>/videos/<id>` permalinks).
- **Modular Provider Architecture (`com.example.extractor`)**:
  - `VideoProvider`: Clean interface defining host matching and asynchronous metadata extraction.
  - `FacebookVideoProvider`: Active v1.0 implementation performing multi-strategy HTTP extraction (Desktop Relay parser, Facebook Video Embed plugin parser, and Mobile Basic fallback) with `HEAD` stream size probing.
  - `TikTokVideoProviderStub` & `ProviderRegistry`: Pre-wired registry demonstrating how TikTok or additional providers can be enabled as independent modules without modifying the UI or download pipeline.
- **Compliance & Legal Boundaries**: VeloStream never attempts to bypass Facebook authentication, login walls, private-video restrictions, age gates, or DRM.

---

## 2. Real Extraction & Streaming Download Workflow

1. **URL Validation & Normalization (`UrlSecurityValidator`)**:
   - Validates HTTPS/HTTP schemes, rejects embedded credentials, blocks SSRF targets, rejects unsupported platforms (TikTok, YouTube, Instagram) with clear messaging, and strips tracking parameters (`fbclid`, `utm_*`, `__cft__`, `mibextid`).
2. **Real Facebook Metadata Extraction (`FacebookVideoProvider`)**:
   - Queries Facebook endpoints using controlled redirect inspection (up to 5 hops, validating every `Location` target against SSRF rules).
   - Extracts `browser_native_hd_url`, `browser_native_sd_url`, `playable_url_quality_hd`, `playable_url`, `hd_src_no_ratelimit`, `sd_src_no_ratelimit`, OpenGraph video tags, title, publisher name, thumbnail URL, and duration.
   - Probes discovered `fbcdn.net` stream URLs via `HEAD` requests to report genuine `Content-Length` byte sizes.
3. **Chunked Binary Streaming (`VideoStreamDownloader`)**:
   - Streams video data in 32 KB buffers without loading entire video files into memory.
   - Rejects `text/html` responses and verifies binary header bytes so an HTML error page is never saved as a fake `.mp4`.
   - Computes a live SHA-256 digest during streaming, writes to atomic `.part_<timestamp>` temporary files (auto-expiring stale partials older than 30 minutes), and exports completed MP4 files to `MediaStore.Downloads/VeloStream` on Android 10+.

---

## 3. Internationalization (EN / AR RTL / FR) & Theme System

- **Three Complete Languages (`AppStringsRepository`)**:
  - **English (`en`)** — Left-to-Right (`LayoutDirection.Ltr`)
  - **العربية (`ar`)** — Native Right-to-Left (`LayoutDirection.Rtl` via `CompositionLocalProvider(LocalLayoutDirection)`)
  - **Français (`fr`)** — Left-to-Right (`LayoutDirection.Ltr`)
- **Adaptive Light & Dark Modes**:
  - Persists user language and theme selection locally via `SharedPreferences`.

---

## 4. Security Checklist

- [x] Strict URI parsing and length/control-character sanitization.
- [x] SSRF blocking for `localhost`, `127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.169.254`, `0.0.0.0`, IPv6 literals, and internal TLDs (`.local`, `.internal`).
- [x] Manual redirect hop validation (`followRedirects(false)` in `FacebookVideoProvider` with per-hop allowlist & SSRF checks).
- [x] Sliding-window rate limiting (`SlidingWindowRateLimiter`: 8 requests per 60 seconds).
- [x] 500 MB download size ceiling and connect/read timeouts.
- [x] HTML tag stripping and filename sanitization (`sanitizeSafeFilename`, `sanitizeMetadataText`).
- [x] Zero shell command execution (`Runtime.exec` / `ProcessBuilder` are never used).
- [x] Zero hardcoded credentials or cookies.

---

## 5. Local Development, Testing & Deployment

### Prerequisites
- JDK 11+ / Android SDK 36
- Gradle (pre-configured in environment)

### Commands
```bash
# Run unit and Robolectric UI/integration tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```
