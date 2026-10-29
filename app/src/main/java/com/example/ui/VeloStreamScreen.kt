package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.AppLanguage
import com.example.domain.DownloadWorkflowState
import com.example.domain.ExtractionErrorType
import com.example.domain.UrlValidationResult
import com.example.domain.VideoMetadata
import com.example.domain.VideoQualityOption
import com.example.i18n.AppStringsRepository
import com.example.i18n.LocalizedStrings
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun VeloStreamScreen(
    uiState: VeloStreamUiState,
    isDarkTheme: Boolean,
    onUrlChanged: (String) -> Unit,
    onClearUrl: () -> Unit,
    onSubmitExtract: () -> Unit,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onStartDownload: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleTheme: () -> Unit,
    onLoadDiagnosticStream: () -> Unit,
    onOpenDialog: (InfoDialogType) -> Unit,
    onDismissDialog: () -> Unit
) {
    val strings = AppStringsRepository.forLanguage(uiState.language)
    val layoutDirection = if (uiState.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("velostream_root"),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Subtle Cinematic Animated Background
                CinematicHeroBackground(
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // A. Navigation Bar
                    TopNavigationBar(
                        strings = strings,
                        currentLanguage = uiState.language,
                        isDarkTheme = isDarkTheme,
                        onSelectLanguage = onSelectLanguage,
                        onToggleTheme = onToggleTheme,
                        onScrollToHowItWorks = {
                            coroutineScope.launch { scrollState.animateScrollTo(1100) }
                        },
                        onOpenSecurityDialog = { onOpenDialog(InfoDialogType.SECURITY_AND_API) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // B. Hero Header & Main Download Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 840.dp)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HeroHeaderSection(strings = strings)

                        Spacer(modifier = Modifier.height(20.dp))

                        MainDownloadCard(
                            strings = strings,
                            urlInput = uiState.urlInput,
                            inlineValidation = uiState.inlineValidation,
                            workflowState = uiState.workflowState,
                            onUrlChanged = onUrlChanged,
                            onClearUrl = onClearUrl,
                            onSubmitExtract = onSubmitExtract,
                            onLoadDiagnosticStream = onLoadDiagnosticStream
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // C & D. Dynamic Download States & Real Video Preview
                        WorkflowStateSection(
                            strings = strings,
                            workflowState = uiState.workflowState,
                            onSelectQuality = onSelectQuality,
                            onStartDownload = onStartDownload,
                            onRetry = onSubmitExtract,
                            onReset = onClearUrl,
                            onLoadDiagnosticStream = onLoadDiagnosticStream
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        // E. How It Works Section
                        HowItWorksSection(strings = strings)

                        Spacer(modifier = Modifier.height(36.dp))

                        // F. Genuine Features Section
                        FeaturesGridSection(strings = strings)

                        Spacer(modifier = Modifier.height(36.dp))

                        // G. FAQ & Legal Footer Section
                        FaqAndFooterSection(
                            strings = strings,
                            onOpenDialog = onOpenDialog
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // Legal / Security Specification Modal Dialog
                uiState.activeDialog?.let { dialogType ->
                    LegalOrSecurityDialog(
                        dialogType = dialogType,
                        strings = strings,
                        onDismiss = onDismissDialog
                    )
                }
            }
        }
    }
}

@Composable
private fun CinematicHeroBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_bg_motion")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_phase"
    )

    val primaryGlow = if (isDarkTheme) Color(0xFF4F46E5).copy(alpha = 0.22f) else Color(0xFF6366F1).copy(alpha = 0.12f)
    val secondaryGlow = if (isDarkTheme) Color(0xFF0EA5E9).copy(alpha = 0.16f) else Color(0xFF3B82F6).copy(alpha = 0.10f)
    val tertiaryGlow = if (isDarkTheme) Color(0xFF9333EA).copy(alpha = 0.15f) else Color(0xFF8B5CF6).copy(alpha = 0.08f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow, Color.Transparent),
                center = Offset(w * (0.2f + 0.15f * phase), h * 0.12f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * (0.2f + 0.15f * phase), h * 0.12f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryGlow, Color.Transparent),
                center = Offset(w * (0.82f - 0.15f * phase), h * 0.22f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * (0.82f - 0.15f * phase), h * 0.22f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryGlow, Color.Transparent),
                center = Offset(w * 0.5f, h * (0.42f + 0.05f * phase)),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.5f, h * (0.42f + 0.05f * phase))
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopNavigationBar(
    strings: LocalizedStrings,
    currentLanguage: AppLanguage,
    isDarkTheme: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleTheme: () -> Unit,
    onScrollToHowItWorks: () -> Unit,
    onOpenSecurityDialog: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_navigation_bar"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Logo + Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VeloStreamVectorLogo(modifier = Modifier.size(38.dp))
                    Column {
                        Text(
                            text = strings.brandName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("brand_name_text")
                        )
                        Text(
                            text = strings.brandTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Theme Toggle Button (48dp touch target)
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) strings.themeLightLabel else strings.themeDarkLabel,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Language Switcher + Quick Links Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("language_selector_row")
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = lang == currentLanguage
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLanguage(lang) },
                            label = {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onScrollToHowItWorks,
                        modifier = Modifier.testTag("nav_how_it_works_button")
                    ) {
                        Text(
                            text = strings.navHowItWorks,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    TextButton(
                        onClick = onOpenSecurityDialog,
                        modifier = Modifier.testTag("nav_security_spec_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.navSecuritySpec,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VeloStreamVectorLogo(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primary, tertiary)
                )
            )
            .padding(7.dp)
    ) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            // Sleek downward V-arrow prism
            moveTo(w * 0.20f, h * 0.22f)
            lineTo(w * 0.50f, h * 0.72f)
            lineTo(w * 0.80f, h * 0.22f)
            lineTo(w * 0.64f, h * 0.22f)
            lineTo(w * 0.50f, h * 0.48f)
            lineTo(w * 0.36f, h * 0.22f)
            close()
        }
        drawPath(path = path, color = Color.White)
        // Base bar
        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(w * 0.22f, h * 0.80f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
    }
}

@Composable
private fun HeroHeaderSection(strings: LocalizedStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_header_section"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            shape = CircleShape,
            modifier = Modifier.testTag("supported_platform_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.heroPlatformBadge,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = strings.heroHeadline,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("hero_headline")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = strings.heroSubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainDownloadCard(
    strings: LocalizedStrings,
    urlInput: String,
    inlineValidation: UrlValidationResult?,
    workflowState: DownloadWorkflowState,
    onUrlChanged: (String) -> Unit,
    onClearUrl: () -> Unit,
    onSubmitExtract: () -> Unit,
    onLoadDiagnosticStream: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val isBusy = workflowState is DownloadWorkflowState.Extracting ||
        workflowState is DownloadWorkflowState.PreparingDownload ||
        workflowState is DownloadWorkflowState.Downloading

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("main_download_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.inputLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.rateLimitQuotaLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // URL Input Field (always LTR inside the URL text syntax for readability, while respecting RTL container)
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("facebook_url_input"),
                enabled = !isBusy,
                placeholder = {
                    Text(
                        text = strings.inputPlaceholder,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (urlInput.isNotEmpty()) {
                            IconButton(
                                onClick = onClearUrl,
                                modifier = Modifier.testTag("clear_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = strings.clearButton
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    onUrlChanged(clipText.trim())
                                }
                            },
                            modifier = Modifier.testTag("paste_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = strings.pasteButton,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        focusManager.clearFocus()
                        onSubmitExtract()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Real-time normalized URL preview badge when valid
            AnimatedVisibility(
                visible = inlineValidation is UrlValidationResult.Valid,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val valid = inlineValidation as? UrlValidationResult.Valid
                if (valid != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("valid_url_normalized_badge"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Normalized: ${valid.normalizedUrl}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Submit / Extract Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmitExtract()
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("extract_video_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.extractButton,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.supportedFormatsHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Format Chips for Testing Supported Formats
            Text(
                text = strings.sampleFacebookUrlsLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val examples = listOf(
                    "Watch URL" to "https://www.facebook.com/watch/?v=10153231379946729",
                    "fb.watch" to "https://fb.watch/v1d3oId99/",
                    "Share /v/" to "https://www.facebook.com/share/v/15aBcDeFgH/",
                    "Reel URL" to "https://www.facebook.com/reel/849201746291048"
                )
                examples.forEachIndexed { index, (label, sampleUrl) ->
                    AssistChip(
                        onClick = { onUrlChanged(sampleUrl) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("sample_url_chip_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Pipeline Verification Button (transparently labeled)
            OutlinedButton(
                onClick = onLoadDiagnosticStream,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_pipeline_diagnostic_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.livePipelineDiagnosticButton,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
