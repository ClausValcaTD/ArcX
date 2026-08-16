package com.m5dev.arcx.presentation.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m5dev.arcx.domain.model.FileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // General Category
            PreferenceCategoryHeader(title = "General")

            PreferenceItem(
                icon = Icons.Default.Folder,
                title = "Default extract location",
                summary = if (uiState.preferences.defaultExtractLocation.isBlank()) {
                    "Same directory as source archive"
                } else {
                    uiState.preferences.defaultExtractLocation
                },
                onClick = { viewModel.onOpenFolderPicker() }
            )

            PreferenceItem(
                icon = Icons.Outlined.Compress,
                title = "Default compression format",
                summary = uiState.preferences.defaultCompressionFormat,
                onClick = { viewModel.onFormatClick() }
            )

            PreferenceItem(
                icon = Icons.Default.Tune,
                title = "Default compression level",
                summary = uiState.preferences.defaultCompressionLevel,
                onClick = { viewModel.onLevelClick() }
            )

            SwitchPreferenceItem(
                icon = Icons.Outlined.FileCopy,
                title = "Ask before overwrite",
                summary = "Prompt for confirmation before replacing existing files",
                checked = uiState.preferences.askBeforeOverwrite,
                onCheckedChange = { viewModel.onAskBeforeOverwriteToggle(it) }
            )

            // Appearance Category
            PreferenceCategoryHeader(title = "Appearance")

            val themeDisplayName = when (uiState.preferences.theme) {
                "LIGHT" -> "Light"
                "DARK" -> "Dark"
                else -> "System default"
            }
            PreferenceItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                summary = themeDisplayName,
                onClick = { viewModel.onThemeClick() }
            )

            // Dynamic colors toggle (hidden if Android < 12)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchPreferenceItem(
                    icon = Icons.Default.ColorLens,
                    title = "Dynamic colors",
                    summary = "Apply wallpaper colors (Material You)",
                    checked = uiState.preferences.dynamicColors,
                    onCheckedChange = { viewModel.onDynamicColorsToggle(it) }
                )
            }

            SwitchPreferenceItem(
                icon = Icons.Default.Visibility,
                title = "Show hidden files",
                summary = "Display files and folders starting with a dot",
                checked = uiState.preferences.showHiddenFiles,
                onCheckedChange = { viewModel.onShowHiddenFilesToggle(it) }
            )

            // Notifications Category
            PreferenceCategoryHeader(title = "Notifications")

            SwitchPreferenceItem(
                icon = Icons.Default.Notifications,
                title = "Show extraction notifications",
                summary = "Display ongoing progress and result alerts",
                checked = uiState.preferences.showExtractionNotifications,
                onCheckedChange = { viewModel.onShowExtractionNotificationsToggle(it) }
            )

            SwitchPreferenceItem(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Show completion sound",
                summary = "Play notification sound when extraction or compression completes",
                checked = uiState.preferences.showCompletionSound,
                onCheckedChange = { viewModel.onShowCompletionSoundToggle(it) }
            )

            SwitchPreferenceItem(
                icon = Icons.Default.Vibration,
                title = "Vibrate on completion",
                summary = "Vibrate device when tasks complete",
                checked = uiState.preferences.vibrateOnCompletion,
                onCheckedChange = { viewModel.onVibrateOnCompletionToggle(it) }
            )

            // About Category
            PreferenceCategoryHeader(title = "About")

            PreferenceItem(
                icon = Icons.Default.Info,
                title = "App version",
                summary = "1.0 (Build 1)",
                onClick = null
            )

            PreferenceItem(
                icon = Icons.Default.Code,
                title = "Open source licenses",
                summary = "Third-party software libraries and components",
                onClick = { viewModel.onLicensesClick() }
            )

            PreferenceItem(
                icon = Icons.Default.Lock,
                title = "Privacy policy",
                summary = "ArcX privacy statement",
                onClick = { viewModel.onPrivacyPolicyClick() }
            )

            PreferenceItem(
                icon = Icons.Default.Code,
                title = "GitHub repository",
                summary = "View source code on GitHub",
                onClick = { viewModel.onGitHubRepoClick(context) }
            )

            PreferenceItem(
                icon = Icons.Default.Star,
                title = "Rate app",
                summary = "Support ArcX on Google Play",
                onClick = { viewModel.onRateAppClick(context) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogs
    if (uiState.showThemeDialog) {
        RadioChoiceDialog(
            title = "Choose Theme",
            options = listOf("SYSTEM" to "System default", "LIGHT" to "Light", "DARK" to "Dark"),
            selectedOption = uiState.preferences.theme,
            onOptionSelected = { viewModel.onSelectTheme(it) },
            onDismiss = { viewModel.onDismissThemeDialog() }
        )
    }

    if (uiState.showFormatDialog) {
        RadioChoiceDialog(
            title = "Default Compression Format",
            options = listOf("ZIP" to "ZIP", "7Z" to "7Z", "TAR" to "TAR"),
            selectedOption = uiState.preferences.defaultCompressionFormat,
            onOptionSelected = { viewModel.onSelectFormat(it) },
            onDismiss = { viewModel.onDismissFormatDialog() }
        )
    }

    if (uiState.showLevelDialog) {
        RadioChoiceDialog(
            title = "Default Compression Level",
            options = listOf(
                "Store" to "Store (No compression)",
                "Fast" to "Fast (Lower ratio, fast)",
                "Normal" to "Normal (Balanced)",
                "Maximum" to "Maximum (Highest ratio)"
            ),
            selectedOption = uiState.preferences.defaultCompressionLevel,
            onOptionSelected = { viewModel.onSelectLevel(it) },
            onDismiss = { viewModel.onDismissLevelDialog() }
        )
    }

    if (uiState.showFolderPickerDialog) {
        FolderPickerDialog(
            currentPath = uiState.folderPickerCurrentPath,
            folders = uiState.folderPickerItems,
            onNavigateTo = { viewModel.onFolderPickerNavigateTo(it) },
            onNavigateUp = { viewModel.onFolderPickerNavigateUp() },
            onSelectCurrentPath = { viewModel.onSelectFolderPickerDestination(it) },
            onResetToDefault = { viewModel.onResetFolderPickerDestinationToDefault() },
            onDismiss = { viewModel.onDismissFolderPicker() }
        )
    }

    if (uiState.showLicensesDialog) {
        LicensesDialog(
            onDismiss = { viewModel.onDismissLicensesDialog() }
        )
    }

    if (uiState.showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { viewModel.onDismissPrivacyDialog() }
        )
    }
}

@Composable
fun PreferenceCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun PreferenceItem(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = summary?.let {
            { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}

@Composable
fun SwitchPreferenceItem(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = summary?.let {
            { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun RadioChoiceDialog(
    title: String,
    options: List<Pair<String, String>>, // key to display text
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(key) }
                            .padding(vertical = 10.dp)
                    ) {
                        RadioButton(
                            selected = key.equals(selectedOption, ignoreCase = true),
                            onClick = { onOptionSelected(key) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FolderPickerDialog(
    currentPath: String,
    folders: List<FileItem>,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSelectCurrentPath: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Select Default Extract Folder", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateUp) {
                        Text(".. (Up Parent Folder)")
                    }
                    TextButton(onClick = onResetToDefault) {
                        Text("Use Source Folder")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (folders.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "No subfolders in this directory",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = folders,
                            key = { it.id }
                        ) { folder ->
                            ListItem(
                                headlineContent = { Text(text = folder.name) },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.clickable { onNavigateTo(folder.path) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelectCurrentPath(currentPath) }
            ) {
                Text("Select This Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LicensesDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Open Source Licenses") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LicenseItem(name = "Libarchive 3.7.7", license = "BSD 2-Clause License")
                LicenseItem(name = "Kotlin Standard Library & Coroutines", license = "Apache License 2.0")
                LicenseItem(name = "Android Jetpack Compose & Material 3", license = "Apache License 2.0")
                LicenseItem(name = "Hilt Dependency Injection", license = "Apache License 2.0")
                LicenseItem(name = "AndroidX DataStore", license = "Apache License 2.0")
                LicenseItem(name = "AndroidX WorkManager", license = "Apache License 2.0")
                LicenseItem(name = "Room Persistence Library", license = "Apache License 2.0")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun LicenseItem(name: String, license: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(text = license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Privacy Policy") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ArcX values your privacy. All archive extraction, compression, and file management operations are processed locally on your device.\n\nArcX does not collect, track, store, or transmit your personal data or files to external servers or third parties.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
