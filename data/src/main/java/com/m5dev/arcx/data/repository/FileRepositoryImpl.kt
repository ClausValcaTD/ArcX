package com.m5dev.arcx.data.repository

import android.os.Environment
import com.m5dev.arcx.data.ndk.ArchiveNative
import com.m5dev.arcx.domain.model.FileItem
import com.m5dev.arcx.domain.model.FileType
import com.m5dev.arcx.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor() : FileRepository {

    override suspend fun getFilesForPath(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Path does not exist: $path"))
            }
            if (!directory.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Path is not a directory: $path"))
            }

            val files = directory.listFiles()
                ?: return@withContext Result.failure(SecurityException("Cannot list files for path: $path"))

            val items = files.map { file ->
                mapToFileItem(file)
            }.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase(Locale.ROOT) }))

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createArchiveWithProgress(
        sourcePaths: List<String>,
        destArchivePath: String,
        format: String,
        level: String,
        password: String?,
        encryptionMethod: String?,
        onProgress: ((current: Int, total: Int, fileName: String) -> Boolean)?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (sourcePaths.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No source files selected for compression"))
            }

            val destFile = File(destArchivePath)
            val parentDir = destFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            val success = ArchiveNative.createArchiveWithProgress(
                sourcePaths = sourcePaths.toTypedArray(),
                destArchivePath = destArchivePath,
                format = format,
                level = level,
                password = password,
                encryptionMethod = encryptionMethod,
                listener = if (onProgress != null) {
                    com.m5dev.arcx.data.ndk.CompressionProgressListener { current, total, fileName ->
                        onProgress.invoke(current, total, fileName)
                    }
                } else null
            )

            if (success) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Failed to create archive or operation was canceled"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAvailableSpaceInBytes(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.freeSpace
            } else {
                file.parentFile?.freeSpace ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun calculateTotalSize(paths: List<String>): Long = withContext(Dispatchers.IO) {
        var total = 0L
        for (p in paths) {
            val file = File(p)
            if (file.exists()) {
                total += calculateFileSizeRecursive(file)
            }
        }
        total
    }

    private fun calculateFileSizeRecursive(file: File): Long {
        if (!file.isDirectory) return file.length()
        var size = 0L
        val children = file.listFiles() ?: return 0L
        for (child in children) {
            size += calculateFileSizeRecursive(child)
        }
        return size
    }

    override suspend fun deleteFile(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist: $path"))
            }
            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Failed to delete $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameFile(path: String, newName: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(path)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Source file does not exist: $path"))
            }
            val targetFile = File(sourceFile.parentFile, newName)
            if (targetFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Target file already exists: ${targetFile.absolutePath}"))
            }
            val renamed = sourceFile.renameTo(targetFile)
            if (renamed) {
                Result.success(mapToFileItem(targetFile))
            } else {
                Result.failure(IllegalStateException("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStorageRootPath(): String {
        return Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
    }

    override fun getDownloadsPath(): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return downloadsDir?.absolutePath ?: "${getStorageRootPath()}/Download"
    }

    override suspend fun extractArchive(
        archivePath: String,
        destPath: String,
        password: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val archiveFile = File(archivePath)
            if (!archiveFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Archive file does not exist: $archivePath"))
            }
            val destDir = File(destPath)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            val success = if (password.isNullOrEmpty()) {
                ArchiveNative.extractArchive(archivePath, destPath)
            } else {
                ArchiveNative.extractArchiveWithPassword(archivePath, destPath, password)
            }

            if (success) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Failed to extract archive"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractArchiveWithProgress(
        archivePath: String,
        destPath: String,
        password: String?,
        onProgress: ((current: Int, total: Int, fileName: String) -> Boolean)?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val archiveFile = File(archivePath)
            if (!archiveFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Archive file does not exist: $archivePath"))
            }
            val destDir = File(destPath)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            val success = ArchiveNative.extractArchiveWithProgress(
                archivePath = archivePath,
                destPath = destPath,
                password = password,
                listener = if (onProgress != null) {
                    com.m5dev.arcx.data.ndk.ExtractionProgressListener { current, total, fileName ->
                        onProgress.invoke(current, total, fileName)
                    }
                } else null
            )

            if (success) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Failed to extract archive or operation was canceled"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listArchiveContents(archivePath: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val archiveFile = File(archivePath)
            if (!archiveFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Archive file does not exist: $archivePath"))
            }
            val contents = ArchiveNative.listArchiveContents(archivePath)
            Result.success(contents.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToFileItem(file: File): FileItem {
        val isFolder = file.isDirectory
        val extension = if (isFolder) "" else file.extension
        val fileType = determineFileType(file)
        val itemCount = if (isFolder) file.listFiles()?.size else null
        val sizeInBytes = if (isFolder) 0L else file.length()
        val formattedSize = if (isFolder) {
            "${itemCount ?: 0} items"
        } else {
            formatFileSize(sizeInBytes)
        }

        return FileItem(
            id = file.absolutePath,
            name = file.name,
            path = file.absolutePath,
            isFolder = isFolder,
            sizeInBytes = sizeInBytes,
            formattedSize = formattedSize,
            lastModifiedTimestamp = file.lastModified(),
            formattedDate = formatDate(file.lastModified()),
            extension = extension,
            fileType = fileType,
            itemCount = itemCount,
            canRead = file.canRead(),
            canWrite = file.canWrite()
        )
    }

    private fun determineFileType(file: File): FileType {
        if (file.isDirectory) return FileType.FOLDER
        val ext = file.extension.lowercase(Locale.ROOT)
        return when (ext) {
            "zip", "7z", "rar", "tar", "gz", "bz2", "xz", "iso", "tgz", "zst", "cab", "arj" -> FileType.ARCHIVE
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif", "ico" -> FileType.IMAGE
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v" -> FileType.VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "mid" -> FileType.AUDIO
            else -> FileType.OTHER
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return if (index == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[index])
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
