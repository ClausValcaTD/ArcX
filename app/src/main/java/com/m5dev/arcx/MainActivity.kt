package com.m5dev.arcx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m5dev.arcx.notification.ExtractionNotificationHelper
import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserScreen
import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserViewModel
import com.m5dev.arcx.presentation.ui.settings.SettingsScreen
import com.m5dev.arcx.presentation.ui.settings.SettingsViewModel
import com.m5dev.arcx.theme.ArcXTheme
import dagger.hilt.android.AndroidEntryPoint

private enum class Screen {
    FILE_BROWSER,
    SETTINGS
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var fileBrowserViewModel: FileBrowserViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (settingsState.preferences.theme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            val dynamicColor = settingsState.preferences.dynamicColors

            var currentScreen by remember { mutableStateOf(Screen.FILE_BROWSER) }

            ArcXTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.FILE_BROWSER -> {
                            val vm: FileBrowserViewModel = hiltViewModel()
                            fileBrowserViewModel = vm
                            FileBrowserScreen(
                                viewModel = vm,
                                onOpenSettings = { currentScreen = Screen.SETTINGS }
                            )
                        }
                        Screen.SETTINGS -> {
                            BackHandler {
                                currentScreen = Screen.FILE_BROWSER
                            }
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { currentScreen = Screen.FILE_BROWSER }
                            )
                        }
                    }
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ExtractionNotificationHelper.ACTION_OPEN_PATH) {
            val destPath = intent.getStringExtra(ExtractionNotificationHelper.EXTRA_DEST_PATH)
            if (!destPath.isNullOrEmpty()) {
                fileBrowserViewModel?.onNavigateToPath(destPath)
            }
        }
    }
}
