package com.example.extractor

import com.example.domain.ExtractionErrorType
import com.example.domain.VideoMetadata

sealed interface ExtractionOutcome {
    data class Success(val metadata: VideoMetadata) : ExtractionOutcome
    data class Failure(
        val errorType: ExtractionErrorType,
        val technicalDetail: String,
        val httpStatusCode: Int? = null
    ) : ExtractionOutcome
}

interface VideoProvider {
    val providerId: String
    val displayName: String
    val isEnabledInCurrentVersion: Boolean
    fun supportsHost(host: String): Boolean
    suspend fun extractVideoMetadata(normalizedUrl: String, videoIdHint: String?): ExtractionOutcome
}

/**
 * Modular stub for future TikTok expansion (v2 roadmap).
 * Ensures the architecture supports adding TikTok without rewriting the core pipeline.
 */
class TikTokVideoProviderStub : VideoProvider {
    override val providerId: String = "tiktok"
    override val displayName: String = "TikTok"
    override val isEnabledInCurrentVersion: Boolean = false

    override fun supportsHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "tiktok.com" || lower.endsWith(".tiktok.com")
    }

    override suspend fun extractVideoMetadata(
        normalizedUrl: String,
        videoIdHint: String?
    ): ExtractionOutcome {
        return ExtractionOutcome.Failure(
            errorType = ExtractionErrorType.VIDEO_UNAVAILABLE_OR_PRIVATE,
            technicalDetail = "TikTok provider module is registered in VeloStream ProviderRegistry but disabled in v1.0 (Facebook-exclusive release)."
        )
    }
}

class ProviderRegistry(
    private val providers: List<VideoProvider>
) {
    fun findProviderForHost(host: String): VideoProvider? {
        return providers.firstOrNull { it.supportsHost(host) }
    }

    fun activeProviders(): List<VideoProvider> = providers.filter { it.isEnabledInCurrentVersion }
    fun allProviders(): List<VideoProvider> = providers
}
