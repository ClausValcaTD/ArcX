package com.m5dev.arcx.presentation.ui.filebrowser

enum class StorageLocation(val displayName: String) {
    INTERNAL_STORAGE("Internal Storage"),
    SD_CARD("SD Card"),
    DOWNLOADS("Downloads")
}

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val size: String,
    val formattedDate: String,
    val extension: String = ""
)

data class FileBrowserUiState(
    val selectedLocation: StorageLocation = StorageLocation.INTERNAL_STORAGE,
    val pathStack: List<String> = listOf("/storage/emulated/0"),
    val folderNameStack: List<String> = listOf("Internal Storage"),
    val items: List<FileItem> = emptyList(),
    val selectedFileForOptions: FileItem? = null,
    val showCreateArchiveDialog: Boolean = false,
    val snackbarMessage: String? = null
) {
    val currentPath: String
        get() = pathStack.lastOrNull() ?: "/storage/emulated/0"

    val currentFolderName: String
        get() = folderNameStack.lastOrNull() ?: selectedLocation.displayName

    val canNavigateUp: Boolean
        get() = pathStack.size > 1
}
