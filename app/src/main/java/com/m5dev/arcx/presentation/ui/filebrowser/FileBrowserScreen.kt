package com.m5dev.arcx.presentation.ui.filebrowser

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m5dev.arcx.domain.model.FileItem
import com.m5dev.arcx.domain.model.FileType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val hasPermission = checkStoragePermission(context)
        viewModel.onPermissionStatusUpdated(hasPermission)
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        viewModel.onPermissionStatusUpdated(granted)
    }

    fun launchPermissionRequest() {
        requestStoragePermission(
            context = context,
            manageStorageLauncher = manageStorageLauncher,
            permissionsLauncher = permissionsLauncher
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = checkStoragePermission(context)
                viewModel.onPermissionStatusUpdated(hasPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(text = "${uiState.selectedPaths.size} selected")
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear selection"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onCompressSelectedFiles() }) {
                            Icon(
                                imageVector = Icons.Outlined.Compress,
                                contentDescription = "Compress selected"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            } else {
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
                    actions = {
                        IconButton(onClick = { viewModel.onOpenActiveJobsSheet() }) {
                            if (uiState.activeJobsCount > 0) {
                                BadgedBox(
                                    badge = { Badge { Text("${uiState.activeJobsCount}") } }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = "Active Jobs"
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "Active Jobs"
                                )
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        bottomBar = {
            FileBrowserBottomBar(
                selectedLocation = uiState.selectedLocation,
                onLocationSelected = { viewModel.onStorageLocationSelected(it) }
            )
        },
        floatingActionButton = {
            if (uiState.hasStoragePermission && !uiState.isSelectionMode) {
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !uiState.hasStoragePermission -> {
                    PermissionDeniedView(
                        onRequestPermission = { launchPermissionRequest() }
                    )
                }
                uiState.isLoading -> {
                    LoadingView()
                }
                uiState.errorMessage != null -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Unknown error",
                        onRetry = { viewModel.refreshCurrentDirectory() }
                    )
                }
                uiState.items.isEmpty() -> {
                    EmptyFolderView()
                }
                else -> {
                    var displayedCount by remember(uiState.items) { mutableIntStateOf(50.coerceAtMost(uiState.items.size)) }
                    val visibleItems = remember(uiState.items, displayedCount) {
                        uiState.items.take(displayedCount)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = visibleItems,
                            key = { it.id }
                        ) { item ->
                            val isSelected = uiState.selectedPaths.contains(item.path)
                            FileListItem(
                                fileItem = item,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleItemSelection(item)
                                    } else {
                                        viewModel.onFileClick(item)
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.isSelectionMode) {
                                        viewModel.onFileLongClick(item)
                                    } else {
                                        viewModel.toggleItemSelection(item)
                                    }
                                }
                            )
                        }

                        if (displayedCount < uiState.items.size) {
                            item {
                                LaunchedEffect(displayedCount) {
                                    displayedCount = (displayedCount + 50).coerceAtMost(uiState.items.size)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Options
    uiState.selectedFileForOptions?.let { fileItem ->
        FileOptionsBottomSheet(
            fileItem = fileItem,
            onDismiss = { viewModel.onDismissFileOptions() },
            onExtract = { viewModel.onOptionActionExtract(fileItem) },
            onCompress = { viewModel.onOptionActionCompress(fileItem) },
            onDelete = { viewModel.onOptionActionDelete(fileItem) },
            onRename = { viewModel.onOptionActionRename(fileItem) },
            onDetails = { viewModel.onOptionActionDetails(fileItem) }
        )
    }

    // Extract Options Dialog (3 Options)
    if (uiState.showExtractOptionsDialog && uiState.extractOptionsFileItem != null) {
        ExtractOptionsDialog(
            archiveName = uiState.extractOptionsFileItem!!.name,
            onDismiss = { viewModel.onDismissExtractOptionsDialog() },
            onExtractHere = { viewModel.onExtractHere() },
            onExtractToFolder = { viewModel.onExtractToFolder() },
            onAdvanced = { viewModel.onOpenAdvancedExtract() }
        )
    }

    // Advanced Extract Dialog
    if (uiState.showAdvancedExtractDialog && uiState.advancedExtractConfig != null) {
        AdvancedExtractDialog(
            config = uiState.advancedExtractConfig!!,
            onDismiss = { viewModel.onDismissAdvancedExtractDialog() },
            onSubmit = { updatedConfig -> viewModel.onSubmitAdvancedExtract(updatedConfig) }
        )
    }

    // Active Jobs Sheet
    if (uiState.showActiveJobsSheet) {
        ActiveJobsBottomSheet(
            activeJobs = uiState.activeJobs,
            onDismiss = { viewModel.onDismissActiveJobsSheet() },
            onCancelJob = { jobId -> viewModel.onCancelJob(jobId) },
            onOpenFolder = { destPath ->
                viewModel.onDismissActiveJobsSheet()
                viewModel.onNavigateToPath(destPath)
            }
        )
    }

    // Extraction Progress Dialog
    if (uiState.isExtracting) {
        ExtractionProgressDialog(
            fileName = uiState.extractingFileName ?: "archive"
        )
    }

    // Password Prompt Dialog
    if (uiState.showPasswordPrompt) {
        PasswordPromptDialog(
            onDismiss = { viewModel.onDismissPasswordPrompt() },
            onConfirm = { password -> viewModel.onConfirmPasswordExtraction(password) }
        )
    }

    // Delete Confirmation Dialog
    uiState.selectedItemForDelete?.let { item ->
        DeleteConfirmationDialog(
            item = item,
            onDismiss = { viewModel.onDismissDeleteDialog() },
            onConfirm = { viewModel.onConfirmDelete() }
        )
    }

    // Rename Dialog
    uiState.selectedItemForRename?.let { item ->
        RenameDialog(
            item = item,
            onDismiss = { viewModel.onDismissRenameDialog() },
            onRename = { newName -> viewModel.onConfirmRename(newName) }
        )
    }

    // Details Dialog
    uiState.selectedItemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { viewModel.onDismissDetailsDialog() }
        )
    }

    // Permission Explanation Dialog
    if (uiState.shouldShowPermissionExplanation) {
        PermissionExplanationDialog(
            onDismiss = { viewModel.onDismissPermissionExplanation() },
            onGrant = {
                viewModel.onRequestPermissionClick()
                launchPermissionRequest()
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

    // Comprehensive Compression Dialog
    uiState.compressionConfig?.let { config ->
        CompressDialog(
            initialConfig = config,
            onDismiss = { viewModel.onDismissCompressionDialog() },
            onSubmit = { updatedConfig, archiveName ->
                viewModel.onSubmitCompression(updatedConfig, archiveName)
            }
        )
    }

    // Overwrite Confirmation Dialog
    if (uiState.showOverwritePrompt) {
        val pendingConfig = uiState.pendingCompressionConfig
        val ext = when (pendingConfig?.format?.lowercase()) {
            "7z" -> ".7z"
            "tar" -> ".tar"
            else -> ".zip"
        }
        val filename = "${pendingConfig?.defaultName}$ext"

        AlertDialog(
            onDismissRequest = { viewModel.onDismissOverwritePrompt() },
            title = { Text("Archive Already Exists") },
            text = { Text("An archive named \"$filename\" already exists in this directory. Do you want to overwrite it?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmOverwriteCompression() }) {
                    Text("Overwrite", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissOverwritePrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveJobsBottomSheet(
    activeJobs: List<ArchiveJobItem>,
    onDismiss: () -> Unit,
    onCancelJob: (UUID) -> Unit,
    onOpenFolder: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Active & Recent Jobs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (activeJobs.isEmpty()) {
                Text(
                    text = "No active or recent jobs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(
                        items = activeJobs,
                        key = { it.id }
                    ) { job ->
                        ArchiveJobListItem(
                            job = job,
                            onCancelJob = { onCancelJob(job.id) },
                            onOpenFolder = { onOpenFolder(job.destPath) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ArchiveJobListItem(
    job: ArchiveJobItem,
    onCancelJob: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.archiveName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (job.isActive) {
                    IconButton(onClick = onCancelJob) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Job",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (job.status == JobStatus.SUCCEEDED && job.destPath.isNotEmpty()) {
                    TextButton(onClick = onOpenFolder) {
                        Text("Open folder")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (job.status) {
                JobStatus.RUNNING, JobStatus.ENQUEUED -> {
                    LinearProgressIndicator(
                        progress = { if (job.totalFiles > 0) job.percentage / 100f else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (job.totalFiles > 0) {
                            "${if (job.jobType == JobType.COMPRESSION) "Compressing" else "Extracting"} ${job.currentFile}/${job.totalFiles} files (${job.percentage}%)"
                        } else "Starting...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                JobStatus.SUCCEEDED -> {
                    Text(
                        text = "${if (job.jobType == JobType.COMPRESSION) "Compression" else "Extraction"} completed successfully",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                JobStatus.FAILED -> {
                    Text(
                        text = "Failed: ${job.errorMessage ?: "Unknown error"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                JobStatus.CANCELLED -> {
                    Text(
                        text = "Canceled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExtractOptionsDialog(
    archiveName: String,
    onDismiss: () -> Unit,
    onExtractHere: () -> Unit,
    onExtractToFolder: () -> Unit,
    onAdvanced: () -> Unit
) {
    val folderName = archiveName.substringBeforeLast(".")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Extract Options") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose how to extract \"$archiveName\":",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onExtractHere,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Extract Here")
                }

                Button(
                    onClick = onExtractToFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Extract to $folderName/")
                }

                OutlinedButton(
                    onClick = onAdvanced,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Advanced...")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedExtractDialog(
    config: AdvancedExtractConfig,
    onDismiss: () -> Unit,
    onSubmit: (AdvancedExtractConfig) -> Unit
) {
    var destPath by remember { mutableStateOf(config.destPath) }
    var overwriteMode by remember { mutableStateOf(config.overwriteMode) }
    var password by remember { mutableStateOf(config.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf(config.selectedFiles) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Advanced Extraction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Destination Path
                OutlinedTextField(
                    value = destPath,
                    onValueChange = { destPath = it },
                    label = { Text("Destination Path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Overwrite Options
                Column {
                    Text(
                        text = "Overwrite Options",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverwriteMode.entries.forEach { mode ->
                            FilterChip(
                                selected = overwriteMode == mode,
                                onClick = { overwriteMode = mode },
                                label = { Text(mode.displayName) },
                                leadingIcon = if (overwriteMode == mode) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (config.isEncrypted) "Password (Required)" else "Password (Optional)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Extract Selected Files List
                Text(
                    text = "Select Files to Extract",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (config.isLoadingContents) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (config.archiveContents.isEmpty()) {
                    Text(
                        text = if (config.isEncrypted) "Enter password to view contents" else "No files found in archive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            items(
                                items = config.archiveContents,
                                key = { it }
                            ) { file ->
                                val isChecked = selectedFiles.contains(file)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFiles = if (isChecked) {
                                                selectedFiles - file
                                            } else {
                                                selectedFiles + file
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedFiles = if (checked == true) {
                                                selectedFiles + file
                                            } else {
                                                selectedFiles - file
                                            }
                                        }
                                    )
                                    Text(
                                        text = file,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = config.copy(
                        destPath = destPath,
                        overwriteMode = overwriteMode,
                        password = password,
                        selectedFiles = selectedFiles
                    )
                    onSubmit(updated)
                }
            ) {
                Text("Extract")
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
fun ExtractionProgressDialog(fileName: String) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissable while extracting */ },
        title = { Text(text = "Extracting Archive") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Extracting $fileName...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
fun PasswordPromptDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Password Required") },
        text = {
            Column {
                Text(
                    text = "This archive is password protected. Enter password to extract:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (password.isNotEmpty()) {
                        onConfirm(password)
                    }
                }
            ) {
                Text("Extract")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileItem: FileItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val iconTint = when (fileItem.fileType) {
        FileType.FOLDER -> MaterialTheme.colorScheme.primary
        FileType.ARCHIVE -> MaterialTheme.colorScheme.tertiary
        FileType.IMAGE -> MaterialTheme.colorScheme.secondary
        FileType.VIDEO -> MaterialTheme.colorScheme.error
        FileType.AUDIO -> MaterialTheme.colorScheme.primary
        FileType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
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
                if (fileItem.formattedDate.isNotEmpty()) {
                    Text(text = fileItem.formattedDate)
                }
                if (fileItem.formattedSize.isNotEmpty()) {
                    if (fileItem.formattedDate.isNotEmpty()) {
                        Text(text = "•")
                    }
                    Text(text = fileItem.formattedSize)
                }
            }
        },
        leadingContent = {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                FileTypeIcon(
                    fileItem = fileItem,
                    tint = iconTint
                )
            }
        }
    )
}

@Composable
fun FileTypeIcon(
    fileItem: FileItem,
    tint: Color
) {
    when (fileItem.fileType) {
        FileType.FOLDER -> {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
        }
        FileType.ARCHIVE -> {
            ArchiveIconWithBadge(
                extension = fileItem.extension,
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
        }
        FileType.IMAGE -> {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Image",
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
        }
        FileType.VIDEO -> {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = "Video",
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
        }
        FileType.AUDIO -> {
            Icon(
                imageVector = Icons.Default.AudioFile,
                contentDescription = "Audio",
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
        }
        FileType.OTHER -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = "File",
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun ArchiveIconWithBadge(
    extension: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.tertiary
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Archive,
            contentDescription = "Archive",
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
        if (extension.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(
                    text = extension.uppercase().take(4),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                )
            }
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileOptionsBottomSheet(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    onExtract: () -> Unit,
    onCompress: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileTypeIcon(
                    fileItem = fileItem,
                    tint = MaterialTheme.colorScheme.primary
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
                        text = "${fileItem.formattedSize} • ${fileItem.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (fileItem.fileType == FileType.ARCHIVE) {
                ListItem(
                    headlineContent = { Text("Extract") },
                    leadingContent = { Icon(Icons.Outlined.FolderZip, contentDescription = null) },
                    modifier = Modifier.combinedClickable(onClick = onExtract)
                )
            }

            ListItem(
                headlineContent = { Text("Compress") },
                leadingContent = { Icon(Icons.Outlined.Compress, contentDescription = null) },
                modifier = Modifier.combinedClickable(onClick = onCompress)
            )

            ListItem(
                headlineContent = { Text("Rename") },
                leadingContent = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                modifier = Modifier.combinedClickable(onClick = onRename)
            )

            ListItem(
                headlineContent = { Text("Details") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier.combinedClickable(onClick = onDetails)
            )

            ListItem(
                headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.combinedClickable(onClick = onDelete)
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete ${if (item.isFolder) "Folder" else "File"}") },
        text = {
            Text(text = "Are you sure you want to delete \"${item.name}\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
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
fun RenameDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(item.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Rename") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onRename(newName.trim())
                    }
                }
            ) {
                Text("Rename")
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
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "File Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "Name", value = item.name)
                DetailRow(label = "Path", value = item.path)
                DetailRow(label = "Type", value = if (item.isFolder) "Folder" else item.fileType.name)
                if (item.extension.isNotEmpty()) {
                    DetailRow(label = "Extension", value = item.extension.uppercase())
                }
                DetailRow(label = "Size", value = item.formattedSize)
                if (item.lastModifiedTimestamp > 0) {
                    DetailRow(label = "Modified", value = item.formattedDate)
                }
                if (item.itemCount != null) {
                    DetailRow(label = "Items", value = "${item.itemCount}")
                }
                DetailRow(
                    label = "Permissions",
                    value = "${if (item.canRead) "Read" else ""}${if (item.canRead && item.canWrite) " / " else ""}${if (item.canWrite) "Write" else ""}"
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

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PermissionExplanationDialog(
    onDismiss: () -> Unit,
    onGrant: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null) },
        title = { Text(text = "Storage Permission Required") },
        text = {
            Text(text = "ArcX needs access to device storage to read, extract, compress, and manage your files. Please grant storage permissions to continue.")
        },
        confirmButton = {
            Button(onClick = onGrant) {
                Text("Grant Permission")
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
fun PermissionDeniedView(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Storage Access Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "ArcX requires storage permissions to display files, extract archives, and compress data.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Storage Permission")
        }
    }
}

@Composable
fun LoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(8) {
            ShimmerFileListItem()
        }
    }
}

@Composable
fun ShimmerFileListItem() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        ) {}
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            ) {}
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            ) {}
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to read directory",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressDialog(
    initialConfig: CompressionConfig,
    onDismiss: () -> Unit,
    onSubmit: (CompressionConfig, String) -> Unit
) {
    var archiveName by remember { mutableStateOf(initialConfig.defaultName) }
    var format by remember { mutableStateOf(initialConfig.format) } // ZIP, 7Z, TAR
    var level by remember { mutableStateOf(initialConfig.compressionLevel) } // Store, Fast, Normal, Maximum
    var password by remember { mutableStateOf(initialConfig.password) }
    var encryptionMethod by remember { mutableStateOf(initialConfig.encryptionMethod) } // ZipCrypto, AES-256
    var passwordVisible by remember { mutableStateOf(false) }

    val formats = listOf("ZIP", "7Z", "TAR")
    val levels = listOf("Store", "Fast", "Normal", "Maximum")
    val encryptionMethods = listOf("ZipCrypto", "AES-256")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Compress Files") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Archive Name
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    suffix = { Text(".${format.lowercase()}") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Archive Format Selection
                Column {
                    Text(
                        text = "Format",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        formats.forEach { fmt ->
                            FilterChip(
                                selected = format == fmt,
                                onClick = { format = fmt },
                                label = { Text(fmt) },
                                leadingIcon = if (format == fmt) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Compression Level Selection
                Column {
                    Text(
                        text = "Compression Level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        levels.forEach { lvl ->
                            FilterChip(
                                selected = level == lvl,
                                onClick = { level = lvl },
                                label = { Text(lvl, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Password Input (Optional)
                if (format == "ZIP" || format == "7Z") {
                    Column {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (optional)") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Encryption Method for ZIP
                        if (password.isNotEmpty() && format == "ZIP") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Encryption Method",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                encryptionMethods.forEach { method ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { encryptionMethod = method }
                                    ) {
                                        RadioButton(
                                            selected = encryptionMethod == method,
                                            onClick = { encryptionMethod = method }
                                        )
                                        Text(
                                            text = method,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (archiveName.isNotBlank()) {
                        val updatedConfig = initialConfig.copy(
                            format = format,
                            compressionLevel = level,
                            password = password,
                            encryptionMethod = encryptionMethod
                        )
                        onSubmit(updatedConfig, archiveName.trim())
                    }
                }
            ) {
                Text("Compress")
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

fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun requestStoragePermission(
    context: Context,
    manageStorageLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    permissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            manageStorageLauncher.launch(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            manageStorageLauncher.launch(intent)
        }
    } else {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }
}
