package com.m5dev.arcx.presentation.ui.filebrowser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = uiState.canNavigateUp) {
        viewModel.onNavigateUp()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.currentFolderName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.currentPath,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.canNavigateUp) {
                        IconButton(onClick = { viewModel.onNavigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            FileBrowserBottomBar(
                selectedLocation = uiState.selectedLocation,
                onLocationSelected = { viewModel.onStorageLocationSelected(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onFabClick() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New Archive"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.items.isEmpty()) {
                EmptyFolderView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.items,
                        key = { it.id }
                    ) { item ->
                        FileListItem(
                            fileItem = item,
                            onClick = {
                                if (item.isFolder) {
                                    viewModel.onFolderClick(item)
                                } else {
                                    viewModel.onFileClick(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for File Options
    uiState.selectedFileForOptions?.let { fileItem ->
        FileOptionsBottomSheet(
            fileItem = fileItem,
            onDismiss = { viewModel.onDismissFileOptions() },
            onActionClick = { actionName ->
                viewModel.onDismissFileOptions()
                viewModel.onCreateArchiveSubmit("$actionName for ${fileItem.name}")
            }
        )
    }

    // Create Archive Dialog
    if (uiState.showCreateArchiveDialog) {
        CreateArchiveDialog(
            onDismiss = { viewModel.onDismissCreateArchiveDialog() },
            onCreate = { name -> viewModel.onCreateArchiveSubmit(name) }
        )
    }
}

@Composable
fun FileListItem(
    fileItem: FileItem,
    onClick: () -> Unit
) {
    val icon: ImageVector
    val iconTint = if (fileItem.isFolder) {
        MaterialTheme.colorScheme.primary
    } else if (isArchiveExtension(fileItem.extension)) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    icon = when {
        fileItem.isFolder -> Icons.Default.Folder
        isArchiveExtension(fileItem.extension) -> Icons.Default.Archive
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = fileItem.name,
                fontWeight = if (fileItem.isFolder) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = fileItem.formattedDate)
                if (fileItem.size.isNotEmpty()) {
                    Text(text = "•")
                    Text(text = fileItem.size)
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = if (fileItem.isFolder) "Folder" else "File",
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }
    )
}

@Composable
fun FileBrowserBottomBar(
    selectedLocation: StorageLocation,
    onLocationSelected: (StorageLocation) -> Unit
) {
    NavigationBar {
        StorageLocation.entries.forEach { location ->
            val icon = when (location) {
                StorageLocation.INTERNAL_STORAGE -> Icons.Default.PhoneAndroid
                StorageLocation.SD_CARD -> Icons.Default.SdCard
                StorageLocation.DOWNLOADS -> Icons.Default.Download
            }

            NavigationBarItem(
                selected = selectedLocation == location,
                onClick = { onLocationSelected(location) },
                icon = { Icon(imageVector = icon, contentDescription = location.displayName) },
                label = { Text(text = location.displayName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileOptionsBottomSheet(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    onActionClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isArchiveExtension(fileItem.extension)) Icons.Default.Archive else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = fileItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${fileItem.size} • ${fileItem.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            if (isArchiveExtension(fileItem.extension)) {
                ListItem(
                    headlineContent = { Text("Extract Here") },
                    leadingContent = { Icon(Icons.Outlined.FolderZip, contentDescription = null) },
                    modifier = Modifier.clickable { onActionClick("Extract Here") }
                )
                ListItem(
                    headlineContent = { Text("Extract to folder...") },
                    leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.clickable { onActionClick("Extract to folder") }
                )
            } else {
                ListItem(
                    headlineContent = { Text("Compress to Archive") },
                    leadingContent = { Icon(Icons.Outlined.Compress, contentDescription = null) },
                    modifier = Modifier.clickable { onActionClick("Compress to Archive") }
                )
            }

            ListItem(
                headlineContent = { Text("Share") },
                leadingContent = { Icon(Icons.Outlined.Share, contentDescription = null) },
                modifier = Modifier.clickable { onActionClick("Share") }
            )
            ListItem(
                headlineContent = { Text("File Properties") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier.clickable { onActionClick("File Properties") }
            )
            ListItem(
                headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onActionClick("Delete") }
            )
        }
    }
}

@Composable
fun CreateArchiveDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var archiveName by remember { mutableStateOf("New_Archive") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Create New Archive") },
        text = {
            Column {
                Text(
                    text = "Enter archive name:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    label = { Text("Archive Name") },
                    suffix = { Text(".zip") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (archiveName.isNotBlank()) {
                        onCreate(archiveName.trim())
                    }
                }
            ) {
                Text("Create")
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
fun EmptyFolderView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Folder is empty",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isArchiveExtension(extension: String): Boolean {
    return extension.lowercase() in listOf("zip", "7z", "rar", "tar", "gz", "bz2", "xz", "iso", "tgz")
}
