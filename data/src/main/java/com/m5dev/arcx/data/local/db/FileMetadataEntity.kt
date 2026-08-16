package com.m5dev.arcx.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_metadata")
data class FileMetadataEntity(
    @PrimaryKey val path: String,
    val parentPath: String,
    val name: String,
    val isFolder: Boolean,
    val sizeInBytes: Long,
    val formattedSize: String,
    val lastModifiedTimestamp: Long,
    val formattedDate: String,
    val extension: String,
    val fileType: String,
    val itemCount: Int?,
    val canRead: Boolean,
    val canWrite: Boolean,
    val cachedAtTimestamp: Long
)
