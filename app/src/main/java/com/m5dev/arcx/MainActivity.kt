package com.m5dev.arcx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.m5dev.arcx.notification.ExtractionNotificationHelper
import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserScreen
import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserViewModel
import com.m5dev.arcx.theme.ArcXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var viewModel: FileBrowserViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArcXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val vm: FileBrowserViewModel = hiltViewModel()
                    viewModel = vm
                    FileBrowserScreen(viewModel = vm)
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
                viewModel?.onNavigateToPath(destPath)
            }
        }
    }
}
