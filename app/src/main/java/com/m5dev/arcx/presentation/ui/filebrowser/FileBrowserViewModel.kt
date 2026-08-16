package com.m5dev.arcx.presentation.ui.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m5dev.arcx.domain.model.FileItem
import com.m5dev.arcx.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    init {
        val rootPath = fileRepository.getStorageRootPath()
        _uiState.update {
            it.copy(
                pathStack = listOf(rootPath),
                folderNameStack = listOf(StorageLocation.INTERNAL_STORAGE.displayName)
            )
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
        val targetDirName = fileItem.name.substringBeforeLast(".")
        val currentPath = _uiState.value.currentPath
        val destPath = "$currentPath/$targetDirName"

        _uiState.update {
            it.copy(
                isExtracting = true,
                extractingFileName = fileItem.name
            )
        }

        viewModelScope.launch {
            val result = fileRepository.extractArchive(
                archivePath = fileItem.path,
                destPath = destPath,
                password = password
            )

            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isExtracting = false,
                            extractingFileName = null,
                            showPasswordPrompt = false,
                            itemForPasswordExtraction = null,
                            snackbarMessage = "Extracted to $targetDirName"
                        )
                    }
                    loadCurrentDirectory()
                },
                onFailure = { error ->
                    val errMsg = error.localizedMessage ?: "Unknown error"
                    val isPassRequired = errMsg.contains("passphrase", ignoreCase = true) ||
                            errMsg.contains("password", ignoreCase = true) ||
                            errMsg.contains("encrypted", ignoreCase = true)

                    if (isPassRequired && password == null) {
                        _uiState.update { state ->
                            state.copy(
                                isExtracting = false,
                                extractingFileName = null,
                                showPasswordPrompt = true,
                                itemForPasswordExtraction = fileItem
                            )
                        }
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                isExtracting = false,
                                extractingFileName = null,
                                showPasswordPrompt = false,
                                itemForPasswordExtraction = null,
                                snackbarMessage = "Extraction failed: $errMsg"
                            )
                        }
                    }
                }
            )
        }
    }

    fun onConfirmPasswordExtraction(password: String) {
        val fileItem = _uiState.value.itemForPasswordExtraction ?: return
        _uiState.update { it.copy(showPasswordPrompt = false) }
        onOptionActionExtract(fileItem, password)
    }

    fun onDismissPasswordPrompt() {
        _uiState.update {
            it.copy(
                showPasswordPrompt = false,
                itemForPasswordExtraction = null
            )
        }
    }

    fun onOptionActionCompress(fileItem: FileItem) {
        onDismissFileOptions()
        _uiState.update {
            it.copy(snackbarMessage = "Compressing ${fileItem.name}...")
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
                    _uiState.update {
                        it.copy(
                            items = fileList,
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
