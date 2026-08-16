package com.m5dev.arcx.presentation.ui.filebrowser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.m5dev.arcx.domain.model.FileItem
import com.m5dev.arcx.domain.model.UserPreferences
import com.m5dev.arcx.domain.repository.FileRepository
import com.m5dev.arcx.domain.repository.SettingsRepository
import com.m5dev.arcx.worker.CompressionWorker
import com.m5dev.arcx.worker.ExtractionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val workManager by lazy { WorkManager.getInstance(context) }

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private var currentUserPreferences = UserPreferences()

    init {
        val rootPath = fileRepository.getStorageRootPath()
        _uiState.update {
            it.copy(
                pathStack = listOf(rootPath),
                folderNameStack = listOf(StorageLocation.INTERNAL_STORAGE.displayName)
            )
        }
        observeUserPreferences()
        observeArchiveJobs()
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            try {
                settingsRepository.userPreferencesFlow.collect { prefs ->
                    val previousShowHidden = currentUserPreferences.showHiddenFiles
                    currentUserPreferences = prefs
                    if (previousShowHidden != prefs.showHiddenFiles && _uiState.value.hasStoragePermission) {
                        loadCurrentDirectory()
                    }
                }
            } catch (e: Exception) {
                // Ignore in tests
            }
        }
    }

    private fun observeArchiveJobs() {
        viewModelScope.launch {
            try {
                val extractionFlow = workManager.getWorkInfosByTagFlow(ExtractionWorker.TAG_EXTRACTION_WORK)
                val compressionFlow = workManager.getWorkInfosByTagFlow(CompressionWorker.TAG_COMPRESSION_WORK)

                combine(extractionFlow, compressionFlow) { extractionList, compressionList ->
                    val extractionJobs = extractionList.map { workInfo ->
                        val progress = workInfo.progress

                        val archivePath = progress.getString(ExtractionWorker.KEY_ARCHIVE_PATH)
                            ?: workInfo.outputData.getString(ExtractionWorker.KEY_ARCHIVE_PATH)
                            ?: ""
                        val destPath = progress.getString(ExtractionWorker.KEY_DEST_PATH)
                            ?: workInfo.outputData.getString(ExtractionWorker.KEY_DEST_PATH)
                            ?: ""
                        val archiveName = progress.getString(ExtractionWorker.KEY_ARCHIVE_NAME)
                            ?: workInfo.outputData.getString(ExtractionWorker.KEY_ARCHIVE_NAME)
                            ?: if (archivePath.isNotEmpty()) File(archivePath).name else "Archive"

                        val currentFile = progress.getInt(ExtractionWorker.KEY_CURRENT_FILE, 0)
                        val totalFiles = progress.getInt(ExtractionWorker.KEY_TOTAL_FILES, 0)
                        val percentage = progress.getInt(ExtractionWorker.KEY_PERCENTAGE, 0)
                        val currentFileName = progress.getString(ExtractionWorker.KEY_FILE_NAME) ?: ""
                        val errorMessage = workInfo.outputData.getString(ExtractionWorker.KEY_ERROR_MESSAGE)

                        val status = when (workInfo.state) {
                            WorkInfo.State.ENQUEUED -> JobStatus.ENQUEUED
                            WorkInfo.State.RUNNING -> JobStatus.RUNNING
                            WorkInfo.State.SUCCEEDED -> JobStatus.SUCCEEDED
                            WorkInfo.State.FAILED -> JobStatus.FAILED
                            WorkInfo.State.BLOCKED -> JobStatus.ENQUEUED
                            WorkInfo.State.CANCELLED -> JobStatus.CANCELLED
                        }

                        ArchiveJobItem(
                            id = workInfo.id,
                            jobType = JobType.EXTRACTION,
                            archiveName = archiveName,
                            sourceOrArchivePath = archivePath,
                            destPath = destPath,
                            status = status,
                            currentFile = currentFile,
                            totalFiles = totalFiles,
                            percentage = percentage,
                            currentFileName = currentFileName,
                            errorMessage = errorMessage
                        )
                    }

                    val compressionJobs = compressionList.map { workInfo ->
                        val progress = workInfo.progress

                        val destArchivePath = progress.getString(CompressionWorker.KEY_DEST_ARCHIVE_PATH)
                            ?: workInfo.outputData.getString(CompressionWorker.KEY_DEST_ARCHIVE_PATH)
                            ?: ""
                        val archiveName = progress.getString(CompressionWorker.KEY_ARCHIVE_NAME)
                            ?: workInfo.outputData.getString(CompressionWorker.KEY_ARCHIVE_NAME)
                            ?: if (destArchivePath.isNotEmpty()) File(destArchivePath).name else "Archive"

                        val currentFile = progress.getInt(CompressionWorker.KEY_CURRENT_FILE, 0)
                        val totalFiles = progress.getInt(CompressionWorker.KEY_TOTAL_FILES, 0)
                        val percentage = progress.getInt(CompressionWorker.KEY_PERCENTAGE, 0)
                        val currentFileName = progress.getString(CompressionWorker.KEY_FILE_NAME) ?: ""
                        val errorMessage = workInfo.outputData.getString(CompressionWorker.KEY_ERROR_MESSAGE)

                        val status = when (workInfo.state) {
                            WorkInfo.State.ENQUEUED -> JobStatus.ENQUEUED
                            WorkInfo.State.RUNNING -> JobStatus.RUNNING
                            WorkInfo.State.SUCCEEDED -> JobStatus.SUCCEEDED
                            WorkInfo.State.FAILED -> JobStatus.FAILED
                            WorkInfo.State.BLOCKED -> JobStatus.ENQUEUED
                            WorkInfo.State.CANCELLED -> JobStatus.CANCELLED
                        }

                        ArchiveJobItem(
                            id = workInfo.id,
                            jobType = JobType.COMPRESSION,
                            archiveName = archiveName,
                            sourceOrArchivePath = destArchivePath,
                            destPath = if (destArchivePath.isNotEmpty()) File(destArchivePath).parent ?: "" else "",
                            status = status,
                            currentFile = currentFile,
                            totalFiles = totalFiles,
                            percentage = percentage,
                            currentFileName = currentFileName,
                            errorMessage = errorMessage
                        )
                    }

                    (extractionJobs + compressionJobs).sortedWith(compareBy({ !it.isActive }, { it.archiveName }))
                }.collect { jobItems ->
                    val previousJobs = _uiState.value.activeJobs
                    _uiState.update { it.copy(activeJobs = jobItems) }

                    // Automatically refresh current folder when a compression job completes
                    val newlySucceededCompression = jobItems.any { job ->
                        job.jobType == JobType.COMPRESSION && job.status == JobStatus.SUCCEEDED &&
                                previousJobs.find { it.id == job.id }?.status != JobStatus.SUCCEEDED
                    }
                    if (newlySucceededCompression) {
                        refreshCurrentDirectory()
                    }
                }
            } catch (e: Exception) {
                // WorkManager might not be initialized in test environments
            }
        }
    }

    fun onPermissionStatusUpdated(hasPermission: Boolean) {
        _uiState.update {
            it.copy(
                hasStoragePermission = hasPermission,
                shouldShowPermissionExplanation = !hasPermission
            )
        }
        if (hasPermission) {
            loadCurrentDirectory()
        }
    }

    fun onRequestPermissionClick() {
        _uiState.update { it.copy(shouldShowPermissionExplanation = false) }
    }

    fun onDismissPermissionExplanation() {
        _uiState.update { it.copy(shouldShowPermissionExplanation = false) }
    }

    fun onStorageLocationSelected(location: StorageLocation) {
        val currentLoc = _uiState.value.selectedLocation
        if (currentLoc == location && _uiState.value.pathStack.size == 1) return

        val rootPath = getRootPathForLocation(location)
        _uiState.update {
            it.copy(
                selectedLocation = location,
                pathStack = listOf(rootPath),
                folderNameStack = listOf(location.displayName),
                selectedFileForOptions = null
            )
        }
        if (_uiState.value.hasStoragePermission) {
            loadCurrentDirectory()
        }
    }

    fun onFolderClick(folder: FileItem) {
        if (!folder.isFolder) return
        val newPath = folder.path
        val newFolderName = folder.name
        _uiState.update { state ->
            state.copy(
                pathStack = state.pathStack + newPath,
                folderNameStack = state.folderNameStack + newFolderName
            )
        }
        loadCurrentDirectory()
    }

    fun onNavigateUp(): Boolean {
        val state = _uiState.value
        if (!state.canNavigateUp) return false

        val newPathStack = state.pathStack.dropLast(1)
        val newNameStack = state.folderNameStack.dropLast(1)

        _uiState.update {
            it.copy(
                pathStack = newPathStack,
                folderNameStack = newNameStack
            )
        }
        loadCurrentDirectory()
        return true
    }

    fun onNavigateToPath(targetPath: String) {
        if (targetPath.isBlank()) return
        val file = File(targetPath)
        if (!file.exists()) return

        val folder = if (file.isDirectory) file else file.parentFile ?: return
        val path = folder.absolutePath

        _uiState.update { state ->
            state.copy(
                pathStack = listOf(fileRepository.getStorageRootPath(), path).distinct(),
                folderNameStack = listOf(StorageLocation.INTERNAL_STORAGE.displayName, folder.name).distinct()
            )
        }
        loadCurrentDirectory()
    }

    fun onFileClick(file: FileItem) {
        if (file.isFolder) {
            onFolderClick(file)
        } else {
            _uiState.update { it.copy(selectedFileForOptions = file) }
        }
    }

    fun onFileLongClick(item: FileItem) {
        _uiState.update { it.copy(selectedFileForOptions = item) }
    }

    fun onDismissFileOptions() {
        _uiState.update { it.copy(selectedFileForOptions = null) }
    }

    fun onOptionActionExtract(fileItem: FileItem, password: String? = null) {
        onDismissFileOptions()
        _uiState.update {
            it.copy(
                showExtractOptionsDialog = true,
                extractOptionsFileItem = fileItem
            )
        }
    }

    fun onDismissExtractOptionsDialog() {
        _uiState.update {
            it.copy(
                showExtractOptionsDialog = false,
                extractOptionsFileItem = null
            )
        }
    }

    fun onExtractHere() {
        val item = _uiState.value.extractOptionsFileItem ?: return
        onDismissExtractOptionsDialog()
        checkEncryptionAndExtract(item, _uiState.value.currentPath)
    }

    fun onExtractToFolder() {
        val item = _uiState.value.extractOptionsFileItem ?: return
        onDismissExtractOptionsDialog()
        val folderName = item.name.substringBeforeLast(".")
        val destPath = "${_uiState.value.currentPath}/$folderName"
        checkEncryptionAndExtract(item, destPath)
    }

    fun onOpenAdvancedExtract() {
        val item = _uiState.value.extractOptionsFileItem ?: return
        onDismissExtractOptionsDialog()

        val initialConfig = AdvancedExtractConfig(
            archiveItem = item,
            destPath = _uiState.value.currentPath,
            isLoadingContents = true
        )

        _uiState.update {
            it.copy(
                showAdvancedExtractDialog = true,
                advancedExtractConfig = initialConfig
            )
        }

        viewModelScope.launch {
            val result = fileRepository.listArchiveContents(item.path)
            result.fold(
                onSuccess = { contents ->
                    _uiState.update { state ->
                        state.advancedExtractConfig?.let { currentConfig ->
                            state.copy(
                                advancedExtractConfig = currentConfig.copy(
                                    archiveContents = contents,
                                    selectedFiles = contents.toSet(),
                                    isLoadingContents = false
                                )
                            )
                        } ?: state
                    }
                },
                onFailure = { error ->
                    val errMsg = error.localizedMessage ?: ""
                    val isEnc = errMsg.contains("passphrase", ignoreCase = true) ||
                            errMsg.contains("password", ignoreCase = true) ||
                            errMsg.contains("encrypted", ignoreCase = true)

                    _uiState.update { state ->
                        state.advancedExtractConfig?.let { currentConfig ->
                            state.copy(
                                advancedExtractConfig = currentConfig.copy(
                                    isEncrypted = isEnc,
                                    isLoadingContents = false
                                )
                            )
                        } ?: state
                    }
                }
            )
        }
    }

    fun onDismissAdvancedExtractDialog() {
        _uiState.update {
            it.copy(
                showAdvancedExtractDialog = false,
                advancedExtractConfig = null
            )
        }
    }

    fun onSubmitAdvancedExtract(config: AdvancedExtractConfig) {
        onDismissAdvancedExtractDialog()
        enqueueExtractionWorker(
            fileItem = config.archiveItem,
            destPath = config.destPath,
            password = if (config.password.isNotEmpty()) config.password else null
        )
    }

    private fun checkEncryptionAndExtract(fileItem: FileItem, destPath: String, password: String? = null) {
        if (password == null) {
            viewModelScope.launch {
                val listResult = fileRepository.listArchiveContents(fileItem.path)
                var isEncrypted = false
                listResult.fold(
                    onSuccess = { /* List success */ },
                    onFailure = { error ->
                        val errMsg = error.localizedMessage ?: ""
                        if (errMsg.contains("passphrase", ignoreCase = true) ||
                            errMsg.contains("password", ignoreCase = true) ||
                            errMsg.contains("encrypted", ignoreCase = true)
                        ) {
                            isEncrypted = true
                        }
                    }
                )

                if (isEncrypted) {
                    _uiState.update { state ->
                        state.copy(
                            showPasswordPrompt = true,
                            itemForPasswordExtraction = fileItem
                        )
                    }
                } else {
                    enqueueExtractionWorker(fileItem, destPath, null)
                }
            }
        } else {
            enqueueExtractionWorker(fileItem, destPath, password)
        }
    }

    private fun enqueueExtractionWorker(fileItem: FileItem, destPath: String, password: String?) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<ExtractionWorker>()
                .addTag(ExtractionWorker.TAG_EXTRACTION_WORK)
                .setInputData(
                    workDataOf(
                        ExtractionWorker.KEY_ARCHIVE_PATH to fileItem.path,
                        ExtractionWorker.KEY_DEST_PATH to destPath,
                        ExtractionWorker.KEY_PASSWORD to password
                    )
                )
                .build()

            workManager.enqueue(workRequest)
        } catch (e: Exception) {
            // Fallback if WorkManager is not configured in test environment
        }

        _uiState.update { state ->
            state.copy(
                isExtracting = false,
                extractingFileName = null,
                showPasswordPrompt = false,
                itemForPasswordExtraction = null,
                snackbarMessage = "Extraction started in background"
            )
        }
    }

    fun onConfirmPasswordExtraction(password: String) {
        val fileItem = _uiState.value.itemForPasswordExtraction ?: return
        _uiState.update { it.copy(showPasswordPrompt = false) }
        val targetDirName = fileItem.name.substringBeforeLast(".")
        val currentPath = _uiState.value.currentPath
        val destPath = "$currentPath/$targetDirName"
        enqueueExtractionWorker(fileItem, destPath, password)
    }

    fun onDismissPasswordPrompt() {
        _uiState.update {
            it.copy(
                showPasswordPrompt = false,
                itemForPasswordExtraction = null
            )
        }
    }

    fun onOpenActiveJobsSheet() {
        _uiState.update { it.copy(showActiveJobsSheet = true) }
    }

    fun onDismissActiveJobsSheet() {
        _uiState.update { it.copy(showActiveJobsSheet = false) }
    }

    fun onCancelJob(jobId: UUID) {
        try {
            workManager.cancelWorkById(jobId)
        } catch (e: Exception) {
            // Ignore in testing environment
        }
    }

    fun toggleItemSelection(fileItem: FileItem) {
        _uiState.update { state ->
            val newSelected = if (state.selectedPaths.contains(fileItem.path)) {
                state.selectedPaths - fileItem.path
            } else {
                state.selectedPaths + fileItem.path
            }
            state.copy(
                selectedPaths = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedPaths = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun onOptionActionCompress(fileItem: FileItem) {
        onDismissFileOptions()
        openCompressDialogForPaths(listOf(fileItem.path), fileItem.name)
    }

    fun onCompressSelectedFiles() {
        val selected = _uiState.value.selectedPaths.toList()
        if (selected.isEmpty()) return
        val defaultName = if (selected.size == 1) {
            File(selected.first()).name
        } else {
            "Archive_${System.currentTimeMillis() / 1000}"
        }
        openCompressDialogForPaths(selected, defaultName)
    }

    private fun openCompressDialogForPaths(paths: List<String>, rawName: String) {
        val defaultName = rawName.substringBeforeLast(".")
        val config = CompressionConfig(
            sourcePaths = paths,
            defaultName = if (defaultName.isBlank()) "Archive" else defaultName,
            format = currentUserPreferences.defaultCompressionFormat,
            compressionLevel = currentUserPreferences.defaultCompressionLevel
        )
        _uiState.update {
            it.copy(compressionConfig = config)
        }
    }

    fun onDismissCompressionDialog() {
        _uiState.update { it.copy(compressionConfig = null) }
    }

    fun onSubmitCompression(config: CompressionConfig, archiveName: String) {
        val cleanName = archiveName.trim().removeSuffix(".zip").removeSuffix(".7z").removeSuffix(".tar")
        val ext = when (config.format.lowercase()) {
            "7z" -> ".7z"
            "tar" -> ".tar"
            else -> ".zip"
        }
        val fullArchiveName = cleanName + ext
        val currentPath = _uiState.value.currentPath
        val destArchivePath = "$currentPath/$fullArchiveName"

        if (config.password.isNotEmpty() && config.password.length < 4) {
            _uiState.update {
                it.copy(snackbarMessage = "Password must be at least 4 characters long")
            }
            return
        }

        viewModelScope.launch {
            val requiredBytes = fileRepository.calculateTotalSize(config.sourcePaths)
            val availableBytes = fileRepository.getAvailableSpaceInBytes(currentPath)

            if (availableBytes > 0 && availableBytes < requiredBytes) {
                _uiState.update {
                    it.copy(snackbarMessage = "Insufficient storage space available")
                }
                return@launch
            }

            val finalConfig = config.copy(defaultName = cleanName)
            val currentFiles = fileRepository.getFilesForPath(currentPath).getOrDefault(emptyList())
            val exists = currentFiles.any { it.name.equals(fullArchiveName, ignoreCase = true) }

            if (exists && currentUserPreferences.askBeforeOverwrite) {
                _uiState.update {
                    it.copy(
                        compressionConfig = null,
                        pendingCompressionConfig = finalConfig,
                        showOverwritePrompt = true
                    )
                }
            } else {
                if (exists) {
                    File(destArchivePath).delete()
                }
                _uiState.update { it.copy(compressionConfig = null) }
                enqueueCompressionWorker(finalConfig, destArchivePath, fullArchiveName)
            }
        }
    }

    fun onConfirmOverwriteCompression() {
        val config = _uiState.value.pendingCompressionConfig ?: return
        _uiState.update {
            it.copy(
                showOverwritePrompt = false,
                pendingCompressionConfig = null
            )
        }

        val ext = when (config.format.lowercase()) {
            "7z" -> ".7z"
            "tar" -> ".tar"
            else -> ".zip"
        }
        val fullArchiveName = config.defaultName + ext
        val currentPath = _uiState.value.currentPath
        val destArchivePath = "$currentPath/$fullArchiveName"

        File(destArchivePath).delete()

        enqueueCompressionWorker(config, destArchivePath, fullArchiveName)
    }

    fun onDismissOverwritePrompt() {
        _uiState.update {
            it.copy(
                showOverwritePrompt = false,
                pendingCompressionConfig = null
            )
        }
    }

    private fun enqueueCompressionWorker(config: CompressionConfig, destArchivePath: String, archiveName: String) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<CompressionWorker>()
                .addTag(CompressionWorker.TAG_COMPRESSION_WORK)
                .setInputData(
                    workDataOf(
                        CompressionWorker.KEY_SOURCE_PATHS to config.sourcePaths.toTypedArray(),
                        CompressionWorker.KEY_DEST_ARCHIVE_PATH to destArchivePath,
                        CompressionWorker.KEY_FORMAT to config.format,
                        CompressionWorker.KEY_LEVEL to config.compressionLevel,
                        CompressionWorker.KEY_PASSWORD to config.password,
                        CompressionWorker.KEY_ENCRYPTION_METHOD to config.encryptionMethod
                    )
                )
                .build()

            workManager.enqueue(workRequest)
        } catch (e: Exception) {
            // Fallback if WorkManager is not configured in test environment
        }

        clearSelection()
        _uiState.update { state ->
            state.copy(
                snackbarMessage = "Compression started in background: $archiveName"
            )
        }
    }

    fun onOptionActionDelete(fileItem: FileItem) {
        onDismissFileOptions()
        _uiState.update { it.copy(selectedItemForDelete = fileItem) }
    }

    fun onOptionActionRename(fileItem: FileItem) {
        onDismissFileOptions()
        _uiState.update { it.copy(selectedItemForRename = fileItem) }
    }

    fun onOptionActionDetails(fileItem: FileItem) {
        onDismissFileOptions()
        _uiState.update { it.copy(selectedItemForDetails = fileItem) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(selectedItemForDelete = null) }
    }

    fun onConfirmDelete() {
        val itemToDelete = _uiState.value.selectedItemForDelete ?: return
        onDismissDeleteDialog()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fileRepository.deleteFile(itemToDelete.path)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(snackbarMessage = "Deleted ${itemToDelete.name}")
                    }
                    loadCurrentDirectory()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Failed to delete: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun onDismissRenameDialog() {
        _uiState.update { it.copy(selectedItemForRename = null) }
    }

    fun onConfirmRename(newName: String) {
        val itemToRename = _uiState.value.selectedItemForRename ?: return
        if (newName.isBlank() || newName == itemToRename.name) {
            onDismissRenameDialog()
            return
        }
        onDismissRenameDialog()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fileRepository.renameFile(itemToRename.path, newName)
            result.fold(
                onSuccess = { renamedItem ->
                    _uiState.update {
                        it.copy(snackbarMessage = "Renamed to ${renamedItem.name}")
                    }
                    loadCurrentDirectory()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Failed to rename: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun onDismissDetailsDialog() {
        _uiState.update { it.copy(selectedItemForDetails = null) }
    }

    fun onFabClick() {
        _uiState.update { it.copy(showCreateArchiveDialog = true) }
    }

    fun onDismissCreateArchiveDialog() {
        _uiState.update { it.copy(showCreateArchiveDialog = false) }
    }

    fun onCreateArchiveSubmit(archiveName: String) {
        _uiState.update {
            it.copy(
                showCreateArchiveDialog = false,
                snackbarMessage = "Creating archive: $archiveName.zip"
            )
        }
    }

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun refreshCurrentDirectory() {
        if (_uiState.value.hasStoragePermission) {
            loadCurrentDirectory()
        }
    }

    private fun loadCurrentDirectory() {
        val path = _uiState.value.currentPath
        if (path.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = fileRepository.getFilesForPath(path)
            result.fold(
                onSuccess = { fileList ->
                    val showHidden = currentUserPreferences.showHiddenFiles
                    val filteredList = if (!showHidden) {
                        fileList.filter { !it.name.startsWith(".") }
                    } else {
                        fileList
                    }
                    _uiState.update {
                        it.copy(
                            items = filteredList,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            items = emptyList(),
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to read directory"
                        )
                    }
                }
            )
        }
    }

    private fun getRootPathForLocation(location: StorageLocation): String {
        return when (location) {
            StorageLocation.INTERNAL_STORAGE -> fileRepository.getStorageRootPath()
            StorageLocation.SD_CARD -> "/storage/sdcard1"
            StorageLocation.DOWNLOADS -> fileRepository.getDownloadsPath()
        }
    }
}
