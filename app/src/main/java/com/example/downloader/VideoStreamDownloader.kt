package com.example.downloader

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.domain.ExtractionErrorType
import com.example.domain.VideoMetadata
import com.example.domain.VideoQualityOption
import com.example.security.UrlSecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

sealed interface StreamDownloadEvent {
    data object Preparing : StreamDownloadEvent

    data class Progress(
        val bytesRead: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long
    ) : StreamDownloadEvent

    data class Completed(
        val savedFilePath: String,
        val exportedPublicUri: String?,
        val actualFileSizeBytes: Long,
        val sha256Prefix: String
    ) : StreamDownloadEvent

    data class Failed(
        val errorType: ExtractionErrorType,
        val technicalDetail: String
    ) : StreamDownloadEvent
}

class VideoStreamDownloader(
    private val context: Context,
    private val httpClient: OkHttpClient = createStreamingClient(),
    private val maxDownloadBytes: Long = 500L * 1024L * 1024L // 500 MB safety ceiling
) {

    fun downloadVideoStream(
        metadata: VideoMetadata,
        quality: VideoQualityOption
    ): Flow<StreamDownloadEvent> = flow {
        emit(StreamDownloadEvent.Preparing)

        val streamUrl = quality.streamUrl
        if (!UrlSecurityValidator.isSafeRedirectOrStreamUrl(
                url = streamUrl,
                allowVerifiedTestHost = metadata.isDiagnosticStream
            )
        ) {
            emit(
                StreamDownloadEvent.Failed(
                    errorType = ExtractionErrorType.HTTP_ERROR,
                    technicalDetail = "Blocked stream URL by SSRF security policy: $streamUrl"
                )
            )
            return@flow
        }

        val downloadsDir = File(context.filesDir, "downloads").apply {
            if (!exists()) mkdirs()
        }
        cleanupExpiredTempFiles(downloadsDir)

        val safeFilename = UrlSecurityValidator.sanitizeSafeFilename(
            title = metadata.title,
            videoId = metadata.videoId,
            resolution = quality.resolution
        )
        val tempFile = File(downloadsDir, "${safeFilename}.part_${System.currentTimeMillis()}")
        val finalFile = File(downloadsDir, safeFilename)

        try {
            val request = Request.Builder()
                .url(streamUrl)
                .get()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
                .header("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(
                        StreamDownloadEvent.Failed(
                            errorType = ExtractionErrorType.HTTP_ERROR,
                            technicalDetail = "Video stream server returned HTTP ${response.code}"
                        )
                    )
                    return@flow
                }

                val contentType = response.header("Content-Type")?.lowercase(Locale.ROOT) ?: ""
                if (contentType.contains("text/html") || contentType.contains("application/xhtml")) {
                    emit(
                        StreamDownloadEvent.Failed(
                            errorType = ExtractionErrorType.INVALID_MEDIA_CONTENT,
                            technicalDetail = "Server returned an HTML page ($contentType) instead of a binary video stream. Refusing to save invalid file."
                        )
                    )
                    return@flow
                }

                val body = response.body ?: run {
                    emit(
                        StreamDownloadEvent.Failed(
                            errorType = ExtractionErrorType.DOWNLOAD_IO_ERROR,
                            technicalDetail = "Empty response body from video CDN."
                        )
                    )
                    return@flow
                }

                val contentLength = body.contentLength().takeIf { it > 0L } ?: quality.fileSizeBytes ?: -1L
                if (contentLength > maxDownloadBytes) {
                    emit(
                        StreamDownloadEvent.Failed(
                            errorType = ExtractionErrorType.FILE_TOO_LARGE,
                            technicalDetail = "Video size (${contentLength / (1024 * 1024)} MB) exceeds maximum safety limit (${maxDownloadBytes / (1024 * 1024)} MB)."
                        )
                    )
                    return@flow
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var totalRead = 0L
                val startTime = System.currentTimeMillis()
                var lastEmitTime = 0L
                val buffer = ByteArray(32 * 1024)
                var firstChunkChecked = false

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break

                            if (!firstChunkChecked && read >= 12) {
                                firstChunkChecked = true
                                val headAscii = String(buffer, 0, minOf(read, 64), Charsets.ISO_8859_1).lowercase(Locale.ROOT)
                                if (headAscii.startsWith("<!doctype html") || headAscii.startsWith("<html")) {
                                    throw IOException("Received HTML markup instead of binary MP4 data")
                                }
                            }

                            totalRead += read
                            if (totalRead > maxDownloadBytes) {
                                throw IOException("Stream exceeded maximum allowed download size limit")
                            }

                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 80L) {
                                lastEmitTime = now
                                val elapsedSec = ((now - startTime) / 1000.0).coerceAtLeast(0.1)
                                val speed = (totalRead / elapsedSec).toLong()
                                emit(
                                    StreamDownloadEvent.Progress(
                                        bytesRead = totalRead,
                                        totalBytes = contentLength,
                                        speedBytesPerSec = speed
                                    )
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (totalRead < 128L) {
                    tempFile.delete()
                    emit(
                        StreamDownloadEvent.Failed(
                            errorType = ExtractionErrorType.INVALID_MEDIA_CONTENT,
                            technicalDetail = "Downloaded stream was truncated or empty ($totalRead bytes)."
                        )
                    )
                    return@flow
                }

                if (finalFile.exists()) {
                    finalFile.delete()
                }
                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }.take(16)
                val exportedUri = exportToSystemDownloads(finalFile, safeFilename)

                emit(
                    StreamDownloadEvent.Completed(
                        savedFilePath = finalFile.absolutePath,
                        exportedPublicUri = exportedUri,
                        actualFileSizeBytes = finalFile.length(),
                        sha256Prefix = sha256Hex
                    )
                )
            }
        } catch (e: IOException) {
            if (tempFile.exists()) tempFile.delete()
            emit(
                StreamDownloadEvent.Failed(
                    errorType = ExtractionErrorType.DOWNLOAD_IO_ERROR,
                    technicalDetail = e.message ?: "I/O error while streaming video file"
                )
            )
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            emit(
                StreamDownloadEvent.Failed(
                    errorType = ExtractionErrorType.DOWNLOAD_IO_ERROR,
                    technicalDetail = e.message ?: "Unexpected error during file download"
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun exportToSystemDownloads(sourceFile: File, displayFileName: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayFileName)
                put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VeloStream")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out, bufferSize = 32 * 1024)
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanupExpiredTempFiles(directory: File) {
        val cutoff = System.currentTimeMillis() - (30 * 60 * 1000L) // 30 minutes expiration
        directory.listFiles()?.forEach { file ->
            if (file.name.contains(".part_") && file.lastModified() < cutoff) {
                runCatching { file.delete() }
            }
        }
    }

    companion object {
        fun createStreamingClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build()
        }
    }
}
