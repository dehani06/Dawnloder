package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.ThemeMode
import com.example.ui.VeloStreamScreen
import com.example.ui.VeloStreamViewModel
import com.example.ui.theme.VeloStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }

        setContent {
            val viewModel: VeloStreamViewModel = viewModel(
                factory = VeloStreamViewModel.createFactory(applicationContext)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(sharedText) {
                if (!sharedText.isNullOrBlank()) {
                    viewModel.onUrlInputChanged(sharedText)
                }
            }

            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            VeloStreamTheme(darkTheme = useDarkTheme) {
                VeloStreamScreen(
                    uiState = uiState,
                    isDarkTheme = useDarkTheme,
                    onUrlChanged = viewModel::onUrlInputChanged,
                    onClearUrl = viewModel::clearUrlInput,
                    onSubmitExtract = viewModel::submitUrlForExtraction,
                    onSelectQuality = viewModel::selectQuality,
                    onStartDownload = viewModel::startVideoDownload,
                    onSelectLanguage = viewModel::selectLanguage,
                    onToggleTheme = { viewModel.toggleTheme(useDarkTheme) },
                    onLoadDiagnosticStream = viewModel::loadLiveDiagnosticPipelineStream,
                    onOpenDialog = viewModel::openDialog,
                    onDismissDialog = viewModel::dismissDialog
                )
            }
        }
    }
}
