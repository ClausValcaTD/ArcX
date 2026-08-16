package com.m5dev.arcx.presentation.ui.filebrowser

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FileBrowserViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    init {
        loadLocationItems(StorageLocation.INTERNAL_STORAGE)
    }

    fun onStorageLocationSelected(location: StorageLocation) {
        if (_uiState.value.selectedLocation == location && _uiState.value.pathStack.size == 1) return

        val rootPath = getRootPathForLocation(location)
        _uiState.update {
            it.copy(
                selectedLocation = location,
                pathStack = listOf(rootPath),
                folderNameStack = listOf(location.displayName),
                items = getMockItemsForPath(rootPath),
                selectedFileForOptions = null
            )
        }
    }

    fun onFolderClick(folder: FileItem) {
        if (!folder.isFolder) return
        val newPath = folder.path
        val newFolderName = folder.name
        _uiState.update { state ->
            val updatedPathStack = state.pathStack + newPath
            val updatedNameStack = state.folderNameStack + newFolderName
            state.copy(
                pathStack = updatedPathStack,
                folderNameStack = updatedNameStack,
                items = getMockItemsForPath(newPath)
            )
        }
    }

    fun onNavigateUp(): Boolean {
        val state = _uiState.value
        if (!state.canNavigateUp) return false

        val newPathStack = state.pathStack.dropLast(1)
        val newNameStack = state.folderNameStack.dropLast(1)
        val currentPath = newPathStack.last()

        _uiState.update {
            it.copy(
                pathStack = newPathStack,
                folderNameStack = newNameStack,
                items = getMockItemsForPath(currentPath)
            )
        }
        return true
    }

    fun onFileClick(file: FileItem) {
        if (file.isFolder) {
            onFolderClick(file)
        } else {
            _uiState.update { it.copy(selectedFileForOptions = file) }
        }
    }

    fun onDismissFileOptions() {
        _uiState.update { it.copy(selectedFileForOptions = null) }
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
                snackbarMessage = "Created mock archive: $archiveName.zip"
            )
        }
    }

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun loadLocationItems(location: StorageLocation) {
        val rootPath = getRootPathForLocation(location)
        _uiState.update {
            it.copy(
                selectedLocation = location,
                pathStack = listOf(rootPath),
                folderNameStack = listOf(location.displayName),
                items = getMockItemsForPath(rootPath)
            )
        }
    }

    private fun getRootPathForLocation(location: StorageLocation): String {
        return when (location) {
            StorageLocation.INTERNAL_STORAGE -> "/storage/emulated/0"
            StorageLocation.SD_CARD -> "/storage/sdcard1"
            StorageLocation.DOWNLOADS -> "/storage/emulated/0/Download"
        }
    }

    private fun getMockItemsForPath(path: String): List<FileItem> {
        return when (path) {
            "/storage/emulated/0" -> listOf(
                FileItem("1", "Documents", "/storage/emulated/0/Documents", true, "5 items", "May 12, 2024"),
                FileItem("2", "Pictures", "/storage/emulated/0/Pictures", true, "3 items", "May 10, 2024"),
                FileItem("3", "Music", "/storage/emulated/0/Music", true, "2 items", "Apr 28, 2024"),
                FileItem("4", "Android_SDK_Guide.pdf", "/storage/emulated/0/Android_SDK_Guide.pdf", false, "4.2 MB", "May 15, 2024", "pdf"),
                FileItem("5", "Backup_2024_05.tar.gz", "/storage/emulated/0/Backup_2024_05.tar.gz", false, "158.4 MB", "May 01, 2024", "gz"),
                FileItem("6", "presentation.pptx", "/storage/emulated/0/presentation.pptx", false, "12.1 MB", "Apr 20, 2024", "pptx")
            )
            "/storage/emulated/0/Documents" -> listOf(
                FileItem("101", "Project_Proposal.pdf", "/storage/emulated/0/Documents/Project_Proposal.pdf", false, "1.8 MB", "May 12, 2024", "pdf"),
                FileItem("102", "Financial_Report.xlsx", "/storage/emulated/0/Documents/Financial_Report.xlsx", false, "850 KB", "May 11, 2024", "xlsx"),
                FileItem("103", "Notes.txt", "/storage/emulated/0/Documents/Notes.txt", false, "12 KB", "May 09, 2024", "txt"),
                FileItem("104", "Archive_2023.zip", "/storage/emulated/0/Documents/Archive_2023.zip", false, "42.5 MB", "May 05, 2024", "zip"),
                FileItem("105", "Backup.7z", "/storage/emulated/0/Documents/Backup.7z", false, "110.2 MB", "Apr 30, 2024", "7z")
            )
            "/storage/emulated/0/Pictures" -> listOf(
                FileItem("201", "Photo_001.jpg", "/storage/emulated/0/Pictures/Photo_001.jpg", false, "3.4 MB", "May 10, 2024", "jpg"),
                FileItem("202", "Photo_002.png", "/storage/emulated/0/Pictures/Photo_002.png", false, "5.1 MB", "May 08, 2024", "png"),
                FileItem("203", "Vacation_Photos.zip", "/storage/emulated/0/Pictures/Vacation_Photos.zip", false, "88.9 MB", "Apr 25, 2024", "zip")
            )
            "/storage/emulated/0/Music" -> listOf(
                FileItem("301", "Song_01.mp3", "/storage/emulated/0/Music/Song_01.mp3", false, "7.8 MB", "Apr 28, 2024", "mp3"),
                FileItem("302", "Album_Tracks.rar", "/storage/emulated/0/Music/Album_Tracks.rar", false, "65.0 MB", "Apr 20, 2024", "rar")
            )
            "/storage/sdcard1" -> listOf(
                FileItem("401", "Backups", "/storage/sdcard1/Backups", true, "2 items", "Apr 15, 2024"),
                FileItem("402", "Movies", "/storage/sdcard1/Movies", true, "2 items", "May 02, 2024"),
                FileItem("403", "Camera_RAW.rar", "/storage/sdcard1/Camera_RAW.rar", false, "512.0 MB", "Mar 11, 2024", "rar"),
                FileItem("404", "system_image.iso", "/storage/sdcard1/system_image.iso", false, "1.2 GB", "Feb 22, 2024", "iso")
            )
            "/storage/sdcard1/Backups" -> listOf(
                FileItem("411", "full_system_backup.iso", "/storage/sdcard1/Backups/full_system_backup.iso", false, "850 MB", "Apr 15, 2024", "iso"),
                FileItem("412", "data.rar", "/storage/sdcard1/Backups/data.rar", false, "230 MB", "Apr 10, 2024", "rar")
            )
            "/storage/sdcard1/Movies" -> listOf(
                FileItem("421", "sample_video.mp4", "/storage/sdcard1/Movies/sample_video.mp4", false, "450 MB", "May 02, 2024", "mp4"),
                FileItem("422", "trailer.mkv", "/storage/sdcard1/Movies/trailer.mkv", false, "120 MB", "May 01, 2024", "mkv")
            )
            "/storage/emulated/0/Download" -> listOf(
                FileItem("501", "Extracted", "/storage/emulated/0/Download/Extracted", true, "2 items", "May 16, 2024"),
                FileItem("502", "ArcX_v1.0.0.apk", "/storage/emulated/0/Download/ArcX_v1.0.0.apk", false, "18.5 MB", "May 18, 2024", "apk"),
                FileItem("503", "dataset_2024.zip", "/storage/emulated/0/Download/dataset_2024.zip", false, "45.3 MB", "May 17, 2024", "zip"),
                FileItem("504", "archive_sample.7z", "/storage/emulated/0/Download/archive_sample.7z", false, "8.7 MB", "May 14, 2024", "7z"),
                FileItem("505", "invoice_may.pdf", "/storage/emulated/0/Download/invoice_may.pdf", false, "350 KB", "May 12, 2024", "pdf")
            )
            "/storage/emulated/0/Download/Extracted" -> listOf(
                FileItem("511", "setup.exe", "/storage/emulated/0/Download/Extracted/setup.exe", false, "15.2 MB", "May 16, 2024", "exe"),
                FileItem("512", "readme.md", "/storage/emulated/0/Download/Extracted/readme.md", false, "4 KB", "May 16, 2024", "md")
            )
            else -> emptyList()
        }
    }
}
