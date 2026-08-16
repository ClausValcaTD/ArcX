package com.m5dev.arcx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserScreen
import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserViewModel
import com.m5dev.arcx.theme.ArcXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArcXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: FileBrowserViewModel = hiltViewModel()
                    FileBrowserScreen(viewModel = viewModel)
                }
            }
        }
    }
}
