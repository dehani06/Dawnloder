package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.DownloadWorkflowState
import com.example.domain.ExtractionErrorType
import com.example.domain.UrlValidationResult
import com.example.domain.VideoMetadata
import com.example.domain.VideoQualityOption
import com.example.i18n.LocalizedStrings
import java.util.Locale

@Composable
fun WorkflowStateSection(
    strings: LocalizedStrings,
    workflowState: DownloadWorkflowState,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onStartDownload: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onLoadDiagnosticStream: () -> Unit
) {
    when (workflowState) {
        is DownloadWorkflowState.Idle -> {
            // No status card needed when idle
        }
        is DownloadWorkflowState.ValidationError -> {
            ValidationErrorCard(
                strings = strings,
                validation = workflowState.validationResult
            )
        }
        is DownloadWorkflowState.RateLimited -> {
            StatusBannerCard(
                testTag = "state_rate_limited_card",
                icon = Icons.Default.WarningAmber,
                accentColor = MaterialTheme.colorScheme.error,
                title = strings.stateRateLimitedTitle,
                message = "${strings.stateRateLimitedMessage} (${workflowState.retryAfterSeconds}s)"
            )
        }
        is DownloadWorkflowState.Extracting -> {
            ExtractingProgressCard(
                strings = strings,
                normalizedUrl = workflowState.normalizedUrl
            )
        }
        is DownloadWorkflowState.MetadataReady -> {
            VideoPreviewAndQualityCard(
                strings = strings,
                metadata = workflowState.metadata,
                selectedQuality = workflowState.selectedQuality,
                onSelectQuality = onSelectQuality,
                onStartDownload = onStartDownload
            )
        }
        is DownloadWorkflowState.PreparingDownload -> {
            PreparingDownloadCard(
                strings = strings,
                metadata = workflowState.metadata,
                quality = workflowState.selectedQuality
            )
        }
        is DownloadWorkflowState.Downloading -> {
            DownloadingProgressCard(
                strings = strings,
                state = workflowState
            )
        }
        is DownloadWorkflowState.DownloadCompleted -> {
            DownloadCompletedCard(
                strings = strings,
                state = workflowState,
                onReset = onReset
            )
        }
        is DownloadWorkflowState.ExtractionFailed -> {
            ExtractionFailedCard(
                strings = strings,
                state = workflowState,
                onRetry = onRetry,
                onLoadDiagnosticStream = onLoadDiagnosticStream
            )
        }
    }
}

@Composable
private fun ValidationErrorCard(
    strings: LocalizedStrings,
    validation: UrlValidationResult
) {
    val (tag, title, message) = when (validation) {
        is UrlValidationResult.Empty -> Triple(
            "state_empty_error_card",
            strings.stateEmptyTitle,
            strings.stateEmptyMessage
        )
        is UrlValidationResult.InvalidFormat -> Triple(
            "state_invalid_url_card",
            strings.stateInvalidUrlTitle,
            strings.stateInvalidUrlMessage
        )
        is UrlValidationResult.UnsupportedProtocol -> Triple(
            "state_unsupported_protocol_card",
            strings.stateUnsupportedProtocolTitle,
            "${strings.stateUnsupportedProtocolMessage} (${validation.protocol}://)"
        )
        is UrlValidationResult.SsrfBlocked -> Triple(
            "state_ssrf_blocked_card",
            strings.stateSsrfBlockedTitle,
            "${strings.stateSsrfBlockedMessage}\n[${validation.reason}]"
        )
        is UrlValidationResult.UnsupportedPlatform -> Triple(
            "state_unsupported_platform_card",
            "${strings.stateUnsupportedPlatformTitle}: ${validation.platformName}",
            "${strings.stateUnsupportedPlatformMessage} (${validation.host})"
        )
        is UrlValidationResult.UnsupportedFacebookUrl -> Triple(
            "state_unsupported_fb_path_card",
            strings.stateUnsupportedFbPathTitle,
            "${strings.stateUnsupportedFbPathMessage} (${validation.path})"
        )
        is UrlValidationResult.Valid -> return
    }

    StatusBannerCard(
        testTag = tag,
        icon = if (validation is UrlValidationResult.SsrfBlocked) Icons.Default.Lock else Icons.Default.ErrorOutline,
        accentColor = MaterialTheme.colorScheme.error,
        title = title,
        message = message
    )
}

@Composable
private fun StatusBannerCard(
    testTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    title: String,
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ExtractingProgressCard(
    strings: LocalizedStrings,
    normalizedUrl: String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("state_extracting_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = strings.stateExtractingTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.stateExtractingSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = normalizedUrl,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoPreviewAndQualityCard(
    strings: LocalizedStrings,
    metadata: VideoMetadata,
    selectedQuality: VideoQualityOption,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onStartDownload: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("video_preview_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Status badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = strings.stateMetadataReadyBadge,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (metadata.isDiagnosticStream) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = strings.diagnosticStreamNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real Thumbnail (when available) + Metadata Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!metadata.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = metadata.thumbnailUrl,
                            contentDescription = metadata.title ?: "Video thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!metadata.title.isNullOrBlank()) {
                        Text(
                            text = metadata.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("extracted_video_title")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (!metadata.authorName.isNullOrBlank()) {
                        Text(
                            text = "${strings.videoAuthorLabel}: ${metadata.authorName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (metadata.durationSeconds != null) {
                        Text(
                            text = "${strings.videoDurationLabel}: ${formatDuration(metadata.durationSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${strings.extractionStrategyLabel}: ${metadata.extractionStrategy}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            // Quality Selection
            Text(
                text = strings.selectQualityLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quality_selector_group")
            ) {
                metadata.qualities.forEach { option ->
                    val isSelected = option.id == selectedQuality.id
                    val sizeText = option.fileSizeBytes?.let { formatBytes(it) } ?: strings.sizeUnknownLabel
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectQuality(option) },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "${option.label} • ${option.format}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sizeText,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        leadingIcon = if (option.isHd) {
                            {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("quality_option_${option.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("download_selected_quality_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.downloadSelectedButton} (${selectedQuality.resolution})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PreparingDownloadCard(
    strings: LocalizedStrings,
    metadata: VideoMetadata,
    quality: VideoQualityOption
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("state_preparing_download_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = strings.statePreparingDownload,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${metadata.title ?: metadata.videoId} • ${quality.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DownloadingProgressCard(
    strings: LocalizedStrings,
    state: DownloadWorkflowState.Downloading
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("state_downloading_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = strings.stateDownloadingTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (state.totalBytes > 0L) {
                val fraction = (state.bytesDownloaded.toFloat() / state.totalBytes.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .testTag("real_byte_progress_bar")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)} (${(fraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${formatBytes(state.speedBytesPerSec)}/s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Honest indeterminate indicator when CDN omits Content-Length
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${strings.stateDownloadingIndeterminate} (${formatBytes(state.bytesDownloaded)})",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun DownloadCompletedCard(
    strings: LocalizedStrings,
    state: DownloadWorkflowState.DownloadCompleted,
    onReset: () -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF16A34A).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .testTag("state_download_completed_card"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = strings.stateDownloadCompletedTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.stateDownloadCompletedSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${strings.savedFilePathLabel}: ${state.exportedUri ?: state.savedFilePath}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.testTag("completed_file_path_text")
                    )
                    Text(
                        text = "${strings.verifiedFileSizeLabel}: ${formatBytes(state.actualFileSizeBytes)} (${state.actualFileSizeBytes} bytes)",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.testTag("completed_file_size_text")
                    )
                    Text(
                        text = "${strings.sha256IntegrityLabel}: ${state.sha256Prefix}...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("completed_sha256_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.exportedUri != null) {
                    Button(
                        onClick = {
                            runCatching {
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(state.exportedUri), "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(viewIntent, state.metadata.title ?: "MP4"))
                            }.onFailure {
                                Toast.makeText(context, state.savedFilePath, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.shareOrOpenVideoButton)
                    }
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("download_another_button")
                ) {
                    Text(strings.downloadAnotherButton)
                }
            }
        }
    }
}

@Composable
private fun ExtractionFailedCard(
    strings: LocalizedStrings,
    state: DownloadWorkflowState.ExtractionFailed,
    onRetry: () -> Unit,
    onLoadDiagnosticStream: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .testTag("state_extraction_failed_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = strings.stateExtractionFailedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.technicalDetail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(10.dp))

            val guidance = when (state.errorType) {
                ExtractionErrorType.NETWORK_ERROR, ExtractionErrorType.TIMEOUT ->
                    strings.stateNetworkErrorGuidance
                else ->
                    strings.statePrivateOrLoginWallGuidance
            }

            Text(
                text = guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_extraction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.retryButton)
                }

                OutlinedButton(
                    onClick = onLoadDiagnosticStream,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.livePipelineDiagnosticButton,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HowItWorksSection(strings: LocalizedStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("how_it_works_section")
    ) {
        Text(
            text = strings.howItWorksTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.howItWorksSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            strings.steps.forEach { step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = step.number,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturesGridSection(strings: LocalizedStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("features_section")
    ) {
        Text(
            text = strings.featuresTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.featuresSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            strings.features.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { feature ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun FaqAndFooterSection(
    strings: LocalizedStrings,
    onOpenDialog: (InfoDialogType) -> Unit
) {
    var expandedIndex by remember { mutableStateOf<Int?>(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("faq_and_footer_section")
    ) {
        Text(
            text = strings.faqTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.faqSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            strings.faqs.forEachIndexed { index, faq ->
                val isExpanded = expandedIndex == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            expandedIndex = if (isExpanded) null else index
                        }
                        .testTag("faq_item_$index"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = faq.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Spacer(modifier = Modifier.height(16.dp))

        // Legal Links & Non-Affiliation Disclaimer Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = { onOpenDialog(InfoDialogType.PRIVACY_POLICY) },
                modifier = Modifier.testTag("footer_privacy_button")
            ) {
                Text(strings.privacyPolicyTitle)
            }
            TextButton(
                onClick = { onOpenDialog(InfoDialogType.TERMS_OF_USE) },
                modifier = Modifier.testTag("footer_terms_button")
            ) {
                Text(strings.termsOfUseTitle)
            }
            TextButton(
                onClick = { onOpenDialog(InfoDialogType.SECURITY_AND_API) },
                modifier = Modifier.testTag("footer_security_button")
            ) {
                Text(strings.navSecuritySpec)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = strings.footerDisclaimer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("footer_disclaimer_text")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.footerContactPlaceholder,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LegalOrSecurityDialog(
    dialogType: InfoDialogType,
    strings: LocalizedStrings,
    onDismiss: () -> Unit
) {
    val (title, contentText) = when (dialogType) {
        InfoDialogType.SECURITY_AND_API ->
            strings.securityArchitectureTitle to strings.securityArchitectureBullets.joinToString("\n\n• ", prefix = "• ")
        InfoDialogType.PRIVACY_POLICY ->
            strings.privacyPolicyTitle to strings.privacyPolicyBody
        InfoDialogType.TERMS_OF_USE ->
            strings.termsOfUseTitle to strings.termsOfUseBody
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = contentText,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_close_button")
            ) {
                Text(strings.closeButton)
            }
        }
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes < 0L) return "Unknown"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
