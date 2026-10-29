package com.example.domain

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val isRtl: Boolean
) {
    EN("en", "English", false),
    AR("ar", "العربية", true),
    FR("fr", "Français", false);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
        }
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class VideoQualityOption(
    val id: String,
    val label: String,
    val resolution: String,
    val format: String = "MP4",
    val streamUrl: String,
    val fileSizeBytes: Long? = null,
    val isHd: Boolean = false,
    val hasAudio: Boolean = true,
    val mimeType: String = "video/mp4"
)

data class VideoMetadata(
    val videoId: String,
    val canonicalUrl: String,
    val providerId: String,
    val title: String?,
    val authorName: String?,
    val durationSeconds: Long?,
    val thumbnailUrl: String?,
    val qualities: List<VideoQualityOption>,
    val extractionStrategy: String,
    val extractedAtMillis: Long = System.currentTimeMillis(),
    val isDiagnosticStream: Boolean = false
)

sealed interface UrlValidationResult {
    data class Valid(
        val normalizedUrl: String,
        val videoIdHint: String?,
        val host: String
    ) : UrlValidationResult

    data object Empty : UrlValidationResult
    data object InvalidFormat : UrlValidationResult
    data class UnsupportedProtocol(val protocol: String) : UrlValidationResult
    data class SsrfBlocked(val reason: String) : UrlValidationResult
    data class UnsupportedPlatform(val platformName: String, val host: String) : UrlValidationResult
    data class UnsupportedFacebookUrl(val host: String, val path: String) : UrlValidationResult
}

enum class ExtractionErrorType {
    NETWORK_ERROR,
    TIMEOUT,
    VIDEO_UNAVAILABLE_OR_PRIVATE,
    NO_STREAMS_FOUND,
    HTTP_ERROR,
    DOWNLOAD_IO_ERROR,
    FILE_TOO_LARGE,
    INVALID_MEDIA_CONTENT
}

sealed interface DownloadWorkflowState {
    data object Idle : DownloadWorkflowState

    data class ValidationError(
        val validationResult: UrlValidationResult,
        val rawInput: String
    ) : DownloadWorkflowState

    data class RateLimited(
        val retryAfterSeconds: Int
    ) : DownloadWorkflowState

    data class Extracting(
        val normalizedUrl: String
    ) : DownloadWorkflowState

    data class MetadataReady(
        val metadata: VideoMetadata,
        val selectedQuality: VideoQualityOption
    ) : DownloadWorkflowState

    data class PreparingDownload(
        val metadata: VideoMetadata,
        val selectedQuality: VideoQualityOption
    ) : DownloadWorkflowState

    data class Downloading(
        val metadata: VideoMetadata,
        val selectedQuality: VideoQualityOption,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long
    ) : DownloadWorkflowState

    data class DownloadCompleted(
        val metadata: VideoMetadata,
        val selectedQuality: VideoQualityOption,
        val savedFilePath: String,
        val exportedUri: String?,
        val actualFileSizeBytes: Long,
        val sha256Prefix: String
    ) : DownloadWorkflowState

    data class ExtractionFailed(
        val normalizedUrl: String,
        val errorType: ExtractionErrorType,
        val technicalDetail: String,
        val httpStatusCode: Int? = null
    ) : DownloadWorkflowState
}
