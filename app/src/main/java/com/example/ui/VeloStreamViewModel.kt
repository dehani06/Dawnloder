package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.AppLanguage
import com.example.domain.DownloadWorkflowState
import com.example.domain.ExtractionErrorType
import com.example.domain.ThemeMode
import com.example.domain.UrlValidationResult
import com.example.domain.VideoMetadata
import com.example.domain.VideoQualityOption
import com.example.downloader.StreamDownloadEvent
import com.example.downloader.VideoStreamDownloader
import com.example.extractor.ExtractionOutcome
import com.example.extractor.FacebookVideoProvider
import com.example.extractor.ProviderRegistry
import com.example.extractor.TikTokVideoProviderStub
import com.example.extractor.VideoProvider
import com.example.security.SlidingWindowRateLimiter
import com.example.security.UrlSecurityValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VeloStreamUiState(
    val urlInput: String = "",
    val inlineValidation: UrlValidationResult? = null,
    val workflowState: DownloadWorkflowState = DownloadWorkflowState.Idle,
    val language: AppLanguage = AppLanguage.EN,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val remainingRateQuota: Int = 8,
    val activeDialog: InfoDialogType? = null
)

enum class InfoDialogType {
    SECURITY_AND_API,
    PRIVACY_POLICY,
    TERMS_OF_USE
}

class VeloStreamViewModel(
    private val prefs: SharedPreferences,
    private val providerRegistry: ProviderRegistry,
    private val streamDownloader: VideoStreamDownloader,
    private val rateLimiter: SlidingWindowRateLimiter = SlidingWindowRateLimiter()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VeloStreamUiState(
            language = AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.EN.code) ?: AppLanguage.EN.code),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
            }.getOrDefault(ThemeMode.DARK),
            remainingRateQuota = rateLimiter.remainingQuota()
        )
    )
    val uiState: StateFlow<VeloStreamUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun onUrlInputChanged(newInput: String) {
        val inlineCheck = if (newInput.isBlank()) {
            null
        } else {
            UrlSecurityValidator.validateAndNormalizeFacebookUrl(newInput)
        }
        _uiState.update { current ->
            val nextWorkflow = if (current.workflowState is DownloadWorkflowState.ValidationError ||
                current.workflowState is DownloadWorkflowState.RateLimited
            ) {
                DownloadWorkflowState.Idle
            } else {
                current.workflowState
            }
            current.copy(
                urlInput = newInput,
                inlineValidation = inlineCheck,
                workflowState = nextWorkflow
            )
        }
    }

    fun clearUrlInput() {
        activeJob?.cancel()
        _uiState.update {
            it.copy(
                urlInput = "",
                inlineValidation = null,
                workflowState = DownloadWorkflowState.Idle
            )
        }
    }

    fun selectLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _uiState.update { it.copy(language = language) }
    }

    fun toggleTheme(currentlyDark: Boolean) {
        val nextMode = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        prefs.edit().putString(KEY_THEME, nextMode.name).apply()
        _uiState.update { it.copy(themeMode = nextMode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun openDialog(type: InfoDialogType) {
        _uiState.update { it.copy(activeDialog = type) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun submitUrlForExtraction() {
        val rawInput = _uiState.value.urlInput
        val validation = UrlSecurityValidator.validateAndNormalizeFacebookUrl(rawInput)

        if (validation !is UrlValidationResult.Valid) {
            _uiState.update {
                it.copy(
                    inlineValidation = validation,
                    workflowState = DownloadWorkflowState.ValidationError(
                        validationResult = validation,
                        rawInput = rawInput
                    )
                )
            }
            return
        }

        val rateDecision = rateLimiter.tryAcquire()
        _uiState.update { it.copy(remainingRateQuota = rateDecision.remaining) }
        if (!rateDecision.allowed) {
            _uiState.update {
                it.copy(
                    workflowState = DownloadWorkflowState.RateLimited(
                        retryAfterSeconds = rateDecision.retryAfterSeconds
                    )
                )
            }
            return
        }

        val provider = providerRegistry.findProviderForHost(validation.host)
        if (provider == null || !provider.isEnabledInCurrentVersion) {
            _uiState.update {
                it.copy(
                    workflowState = DownloadWorkflowState.ValidationError(
                        validationResult = UrlValidationResult.UnsupportedPlatform(
                            platformName = provider?.displayName ?: validation.host,
                            host = validation.host
                        ),
                        rawInput = rawInput
                    )
                )
            }
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    workflowState = DownloadWorkflowState.Extracting(
                        normalizedUrl = validation.normalizedUrl
                    )
                )
            }

            when (val outcome = provider.extractVideoMetadata(validation.normalizedUrl, validation.videoIdHint)) {
                is ExtractionOutcome.Success -> {
                    val firstQuality = outcome.metadata.qualities.first()
                    _uiState.update {
                        it.copy(
                            workflowState = DownloadWorkflowState.MetadataReady(
                                metadata = outcome.metadata,
                                selectedQuality = firstQuality
                            )
                        )
                    }
                }
                is ExtractionOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            workflowState = DownloadWorkflowState.ExtractionFailed(
                                normalizedUrl = validation.normalizedUrl,
                                errorType = outcome.errorType,
                                technicalDetail = outcome.technicalDetail,
                                httpStatusCode = outcome.httpStatusCode
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectQuality(quality: VideoQualityOption) {
        val current = _uiState.value.workflowState
        if (current is DownloadWorkflowState.MetadataReady) {
            _uiState.update {
                it.copy(
                    workflowState = current.copy(selectedQuality = quality)
                )
            }
        }
    }

    fun startVideoDownload() {
        val current = _uiState.value.workflowState
        val (metadata, selectedQuality) = when (current) {
            is DownloadWorkflowState.MetadataReady -> current.metadata to current.selectedQuality
            is DownloadWorkflowState.DownloadCompleted -> current.metadata to current.selectedQuality
            else -> return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            streamDownloader.downloadVideoStream(metadata, selectedQuality).collect { event ->
                when (event) {
                    is StreamDownloadEvent.Preparing -> {
                        _uiState.update {
                            it.copy(
                                workflowState = DownloadWorkflowState.PreparingDownload(
                                    metadata = metadata,
                                    selectedQuality = selectedQuality
                                )
                            )
                        }
                    }
                    is StreamDownloadEvent.Progress -> {
                        _uiState.update {
                            it.copy(
                                workflowState = DownloadWorkflowState.Downloading(
                                    metadata = metadata,
                                    selectedQuality = selectedQuality,
                                    bytesDownloaded = event.bytesRead,
                                    totalBytes = event.totalBytes,
                                    speedBytesPerSec = event.speedBytesPerSec
                                )
                            )
                        }
                    }
                    is StreamDownloadEvent.Completed -> {
                        _uiState.update {
                            it.copy(
                                workflowState = DownloadWorkflowState.DownloadCompleted(
                                    metadata = metadata,
                                    selectedQuality = selectedQuality,
                                    savedFilePath = event.savedFilePath,
                                    exportedUri = event.exportedPublicUri,
                                    actualFileSizeBytes = event.actualFileSizeBytes,
                                    sha256Prefix = event.sha256Prefix
                                )
                            )
                        }
                    }
                    is StreamDownloadEvent.Failed -> {
                        _uiState.update {
                            it.copy(
                                workflowState = DownloadWorkflowState.ExtractionFailed(
                                    normalizedUrl = metadata.canonicalUrl,
                                    errorType = event.errorType,
                                    technicalDetail = event.technicalDetail
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads a verified, publicly accessible open-source MP4 video stream (clearly marked with
     * isDiagnosticStream = true) so users and automated tests can verify the real HTTP binary
     * streaming downloader, progress tracking, SHA-256 integrity check, and MP4 file saving
     * even if Facebook's firewall blocks unauthenticated requests from the current network.
     */
    fun loadLiveDiagnosticPipelineStream() {
        val hdOption = VideoQualityOption(
            id = "diag_hd_720p",
            label = "HD 720p (Verified Public MP4 Stream)",
            resolution = "720p",
            format = "MP4",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            fileSizeBytes = 2_498_563L,
            isHd = true,
            hasAudio = true,
            mimeType = "video/mp4"
        )
        val sdOption = VideoQualityOption(
            id = "diag_sd_480p",
            label = "SD 480p (Verified Public MP4 Stream)",
            resolution = "480p",
            format = "MP4",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            fileSizeBytes = 2_299_653L,
            isHd = false,
            hasAudio = true,
            mimeType = "video/mp4"
        )
        val diagnosticMetadata = VideoMetadata(
            videoId = "live_pipeline_check",
            canonicalUrl = "https://www.facebook.com/watch/?v=10153231379946729",
            providerId = "facebook",
            title = "Open Media Pipeline Verification Stream (H.264/AAC MP4)",
            authorName = "VeloStream Live Stream Verifier",
            durationSeconds = 15L,
            thumbnailUrl = null,
            qualities = listOf(hdOption, sdOption),
            extractionStrategy = "Direct HTTP Binary Stream Verifier",
            isDiagnosticStream = true
        )
        _uiState.update {
            it.copy(
                workflowState = DownloadWorkflowState.MetadataReady(
                    metadata = diagnosticMetadata,
                    selectedQuality = hdOption
                )
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "velostream_prefs"
        private const val KEY_LANGUAGE = "pref_language_code"
        private const val KEY_THEME = "pref_theme_mode"

        fun createFactory(
            context: Context,
            customFacebookProvider: VideoProvider? = null,
            customDownloader: VideoStreamDownloader? = null,
            customRateLimiter: SlidingWindowRateLimiter? = null
        ): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fbProvider = customFacebookProvider ?: FacebookVideoProvider()
            val registry = ProviderRegistry(
                listOf(
                    fbProvider,
                    TikTokVideoProviderStub()
                )
            )
            val downloader = customDownloader ?: VideoStreamDownloader(appContext)
            val limiter = customRateLimiter ?: SlidingWindowRateLimiter()

            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VeloStreamViewModel(
                        prefs = prefs,
                        providerRegistry = registry,
                        streamDownloader = downloader,
                        rateLimiter = limiter
                    ) as T
                }
            }
        }
    }
}
