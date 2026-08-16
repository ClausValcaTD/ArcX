package com.m5dev.arcx.domain.repository

import com.m5dev.arcx.domain.model.FileItem

interface FileRepository {
    suspend fun getFilesForPath(path: String): Result<List<FileItem>>
    suspend fun deleteFile(path: String): Result<Boolean>
    suspend fun renameFile(path: String, newName: String): Result<FileItem>
    fun getStorageRootPath(): String
    fun getDownloadsPath(): String
    suspend fun extractArchive(archivePath: String, destPath: String, password: String? = null): Result<Boolean>
    suspend fun listArchiveContents(archivePath: String): Result<List<String>>
}
