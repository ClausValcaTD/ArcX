package com.m5dev.arcx.domain.model

enum class FileType {
    FOLDER,
    ARCHIVE,
    IMAGE,
    VIDEO,
    AUDIO,
    OTHER
}

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val sizeInBytes: Long,
    val formattedSize: String,
    val lastModifiedTimestamp: Long,
    val formattedDate: String,
    val extension: String,
    val fileType: FileType,
    val itemCount: Int? = null,
    val canRead: Boolean = true,
    val canWrite: Boolean = true
)
