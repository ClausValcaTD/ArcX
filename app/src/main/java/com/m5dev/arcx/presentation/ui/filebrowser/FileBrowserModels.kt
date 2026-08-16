package com.m5dev.arcx.presentation.ui.filebrowser

import com.m5dev.arcx.domain.model.FileItem

enum class StorageLocation(val displayName: String) {
    INTERNAL_STORAGE("Internal Storage"),
    SD_CARD("SD Card"),
    DOWNLOADS("Downloads")
}

data class FileBrowserUiState(
    val selectedLocation: StorageLocation = StorageLocation.INTERNAL_STORAGE,
    val pathStack: List<String> = emptyList(),
    val folderNameStack: List<String> = emptyList(),
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val shouldShowPermissionExplanation: Boolean = false,
    val selectedFileForOptions: FileItem? = null,
    val selectedItemForRename: FileItem? = null,
    val selectedItemForDelete: FileItem? = null,
    val selectedItemForDetails: FileItem? = null,
    val showCreateArchiveDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
) {
    val currentPath: String
        get() = pathStack.lastOrNull() ?: ""

    val currentFolderName: String
        get() = folderNameStack.lastOrNull() ?: selectedLocation.displayName

    val canNavigateUp: Boolean
        get() = pathStack.size > 1
}
