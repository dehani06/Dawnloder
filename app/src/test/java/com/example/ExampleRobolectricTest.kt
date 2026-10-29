package com.example

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ExtractionErrorType
import com.example.domain.VideoMetadata
import com.example.domain.VideoQualityOption
import com.example.downloader.VideoStreamDownloader
import com.example.extractor.ExtractionOutcome
import com.example.extractor.VideoProvider
import com.example.ui.VeloStreamScreen
import com.example.ui.VeloStreamViewModel
import com.example.ui.theme.VeloStreamTheme
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name resource is VeloStream`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("VeloStream", context.getString(R.string.app_name))
    }

    @Test
    fun `homepage renders and supports EN, AR RTL, FR LTR language switching and theme toggling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = VeloStreamViewModel.createFactory(context)
        val viewModel = factory.create(VeloStreamViewModel::class.java)

        composeTestRule.setContent {
            val state by viewModel.uiState.collectAsState()
            VeloStreamTheme(darkTheme = true) {
                VeloStreamScreen(
                    uiState = state,
                    isDarkTheme = true,
                    onUrlChanged = viewModel::onUrlInputChanged,
                    onClearUrl = viewModel::clearUrlInput,
                    onSubmitExtract = viewModel::submitUrlForExtraction,
                    onSelectQuality = viewModel::selectQuality,
                    onStartDownload = viewModel::startVideoDownload,
                    onSelectLanguage = viewModel::selectLanguage,
                    onToggleTheme = { viewModel.toggleTheme(true) },
                    onLoadDiagnosticStream = viewModel::loadLiveDiagnosticPipelineStream,
                    onOpenDialog = viewModel::openDialog,
                    onDismissDialog = viewModel::dismissDialog
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("top_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main_download_card").assertIsDisplayed()

        // Switch to Arabic (RTL)
        composeTestRule.onNodeWithTag("lang_chip_ar").performClick()
        composeTestRule.waitForIdle()
        assertEquals("ar", viewModel.uiState.value.language.code)
        assertTrue(viewModel.uiState.value.language.isRtl)
        composeTestRule.onNodeWithText("فيلو ستريم").assertIsDisplayed()

        // Switch to French (LTR)
        composeTestRule.onNodeWithTag("lang_chip_fr").performClick()
        composeTestRule.waitForIdle()
        assertEquals("fr", viewModel.uiState.value.language.code)
        composeTestRule.onNodeWithText("Analyser la vidéo").performScrollTo().assertIsDisplayed()

        // Switch back to English
        composeTestRule.onNodeWithTag("lang_chip_en").performClick()
        composeTestRule.waitForIdle()
        assertEquals("en", viewModel.uiState.value.language.code)
    }

    @Test
    fun `validation states render accurately for empty, invalid, SSRF, and TikTok URLs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = VeloStreamViewModel.createFactory(context).create(VeloStreamViewModel::class.java)

        composeTestRule.setContent {
            val state by viewModel.uiState.collectAsState()
            VeloStreamTheme(darkTheme = false) {
                VeloStreamScreen(
                    uiState = state,
                    isDarkTheme = false,
                    onUrlChanged = viewModel::onUrlInputChanged,
                    onClearUrl = viewModel::clearUrlInput,
                    onSubmitExtract = viewModel::submitUrlForExtraction,
                    onSelectQuality = viewModel::selectQuality,
                    onStartDownload = viewModel::startVideoDownload,
                    onSelectLanguage = viewModel::selectLanguage,
                    onToggleTheme = { viewModel.toggleTheme(false) },
                    onLoadDiagnosticStream = viewModel::loadLiveDiagnosticPipelineStream,
                    onOpenDialog = viewModel::openDialog,
                    onDismissDialog = viewModel::dismissDialog
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Empty URL submission
        composeTestRule.onNodeWithTag("extract_video_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("state_empty_error_card").performScrollTo().assertIsDisplayed()

        // 2. SSRF attempt submission
        composeTestRule.onNodeWithTag("facebook_url_input").performScrollTo().performTextInput("http://127.0.0.1:8080/admin")
        composeTestRule.onNodeWithTag("extract_video_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("state_ssrf_blocked_card").performScrollTo().assertIsDisplayed()

        // 3. Unsupported platform (TikTok) submission
        composeTestRule.onNodeWithTag("clear_url_button").performClick()
        composeTestRule.onNodeWithTag("facebook_url_input").performTextInput("https://www.tiktok.com/@creator/video/999")
        composeTestRule.onNodeWithTag("extract_video_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("state_unsupported_platform_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `end-to-end metadata extraction and real binary MP4 file download verifies integrity`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create real binary MP4 header + payload bytes (ftypisom box)
        val mp4BinaryPayload = ByteArray(4096) { idx ->
            when (idx) {
                4 -> 'f'.code.toByte()
                5 -> 't'.code.toByte()
                6 -> 'y'.code.toByte()
                7 -> 'p'.code.toByte()
                8 -> 'i'.code.toByte()
                9 -> 's'.code.toByte()
                10 -> 'o'.code.toByte()
                11 -> 'm'.code.toByte()
                else -> (idx % 251).toByte()
            }
        }

        val interceptingClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "video/mp4")
                    .header("Content-Length", mp4BinaryPayload.size.toString())
                    .body(mp4BinaryPayload.toResponseBody("video/mp4".toMediaType()))
                    .build()
            }
            .build()

        val fakeFbProvider = object : VideoProvider {
            override val providerId: String = "facebook"
            override val displayName: String = "Facebook"
            override val isEnabledInCurrentVersion: Boolean = true
            override fun supportsHost(host: String): Boolean = true
            override suspend fun extractVideoMetadata(
                normalizedUrl: String,
                videoIdHint: String?
            ): ExtractionOutcome {
                return ExtractionOutcome.Success(
                    VideoMetadata(
                        videoId = videoIdHint ?: "10153231379946729",
                        canonicalUrl = normalizedUrl,
                        providerId = "facebook",
                        title = "Real Extracted Facebook HD Video",
                        authorName = "Official Creator Page",
                        durationSeconds = 95L,
                        thumbnailUrl = null,
                        qualities = listOf(
                            VideoQualityOption(
                                id = "hd_720p",
                                label = "HD 720p",
                                resolution = "720p",
                                format = "MP4",
                                streamUrl = "https://video.xx.fbcdn.net/v/t42.1790-2/real_720p.mp4",
                                fileSizeBytes = mp4BinaryPayload.size.toLong(),
                                isHd = true
                            ),
                            VideoQualityOption(
                                id = "sd_360p",
                                label = "SD 360p",
                                resolution = "360p",
                                format = "MP4",
                                streamUrl = "https://video.xx.fbcdn.net/v/t42.1790-2/real_360p.mp4",
                                fileSizeBytes = 2048L,
                                isHd = false
                            )
                        ),
                        extractionStrategy = "Facebook Desktop Relay Parser"
                    )
                )
            }
        }

        val downloader = VideoStreamDownloader(context, httpClient = interceptingClient)
        val events = mutableListOf<com.example.downloader.StreamDownloadEvent>()
        val metadata = (fakeFbProvider.extractVideoMetadata("https://www.facebook.com/watch/?v=10153231379946729", "10153231379946729") as ExtractionOutcome.Success).metadata

        downloader.downloadVideoStream(metadata, metadata.qualities.first()).collect { event ->
            events.add(event)
        }

        val completed = events.filterIsInstance<com.example.downloader.StreamDownloadEvent.Completed>().firstOrNull()
        assertTrue("Expected Completed event from VideoStreamDownloader", completed != null)
        assertEquals(4096L, completed!!.actualFileSizeBytes)
        val savedFile = File(completed.savedFilePath)
        assertTrue(savedFile.exists())
        assertEquals(4096L, savedFile.length())
        assertTrue(completed.sha256Prefix.length == 16)
    }

    @Test
    fun `downloader rejects HTML error page masquerading as MP4 stream`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val htmlBody = "<!DOCTYPE html><html><body>Error 403 Forbidden</body></html>"
        val htmlClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "text/html; charset=utf-8")
                    .body(htmlBody.toResponseBody("text/html".toMediaType()))
                    .build()
            }
            .build()

        val downloader = VideoStreamDownloader(context, httpClient = htmlClient)
        val metadata = VideoMetadata(
            videoId = "test_html_reject",
            canonicalUrl = "https://www.facebook.com/watch/?v=123",
            providerId = "facebook",
            title = "Test",
            authorName = null,
            durationSeconds = 10L,
            thumbnailUrl = null,
            qualities = listOf(
                VideoQualityOption(
                    id = "hd",
                    label = "HD",
                    resolution = "720p",
                    streamUrl = "https://video.xx.fbcdn.net/v/fake.mp4"
                )
            ),
            extractionStrategy = "Test"
        )

        val events = mutableListOf<com.example.downloader.StreamDownloadEvent>()
        downloader.downloadVideoStream(metadata, metadata.qualities.first()).collect {
            events.add(it)
        }

        val failed = events.filterIsInstance<com.example.downloader.StreamDownloadEvent.Failed>().firstOrNull()
        assertTrue("Expected Failed event when server returns text/html", failed != null)
        assertEquals(ExtractionErrorType.INVALID_MEDIA_CONTENT, failed!!.errorType)
    }
}
