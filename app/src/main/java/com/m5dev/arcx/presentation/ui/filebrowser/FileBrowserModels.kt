package com.m5dev.arcx.presentation.ui.filebrowser

import com.m5dev.arcx.domain.model.FileItem
import java.util.UUID

enum class StorageLocation(val displayName: String) {
    INTERNAL_STORAGE("Internal Storage"),
    SD_CARD("SD Card"),
    DOWNLOADS("Downloads")
}

enum class JobType {
    EXTRACTION,
    COMPRESSION
}

enum class JobStatus {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

data class ArchiveJobItem(
    val id: UUID,
    val jobType: JobType,
    val archiveName: String,
    val sourceOrArchivePath: String,
    val destPath: String,
    val status: JobStatus,
    val currentFile: Int = 0,
    val totalFiles: Int = 0,
    val percentage: Int = 0,
    val currentFileName: String = "",
    val errorMessage: String? = null
) {
    val isActive: Boolean
        get() = status == JobStatus.RUNNING || status == JobStatus.ENQUEUED
}

data class CompressionConfig(
    val sourcePaths: List<String>,
    val defaultName: String,
    val format: String = "ZIP", // ZIP, 7Z, TAR
    val compressionLevel: String = "Normal", // Store, Fast, Normal, Maximum
    val password: String = "",
    val encryptionMethod: String = "AES-256" // ZipCrypto or AES-256
)

data class FileBrowserUiState(
    val selectedLocation: StorageLocation = StorageLocation.INTERNAL_STORAGE,
    val pathStack: List<String> = emptyList(),
    val folderNameStack: List<String> = emptyList(),
    val items: List<FileItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val extractingFileName: String? = null,
    val showPasswordPrompt: Boolean = false,
    val itemForPasswordExtraction: FileItem? = null,
    val hasStoragePermission: Boolean = false,
    val shouldShowPermissionExplanation: Boolean = false,
    val selectedFileForOptions: FileItem? = null,
    val selectedItemForRename: FileItem? = null,
    val selectedItemForDelete: FileItem? = null,
    val selectedItemForDetails: FileItem? = null,
    val showCreateArchiveDialog: Boolean = false,
    val compressionConfig: CompressionConfig? = null,
    val pendingCompressionConfig: CompressionConfig? = null,
    val showOverwritePrompt: Boolean = false,
    val showActiveJobsSheet: Boolean = false,
    val activeJobs: List<ArchiveJobItem> = emptyList(),
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
) {
    val currentPath: String
        get() = pathStack.lastOrNull() ?: ""

    val currentFolderName: String
        get() = folderNameStack.lastOrNull() ?: selectedLocation.displayName

    val canNavigateUp: Boolean
        get() = pathStack.size > 1

    val activeJobsCount: Int
        get() = activeJobs.count { it.isActive }
}
