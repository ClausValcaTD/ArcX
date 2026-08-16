package com.m5dev.arcx.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileMetadataDao {
    @Query("SELECT * FROM file_metadata WHERE parentPath = :parentPath ORDER BY isFolder DESC, name ASC")
    suspend fun getFilesForParent(parentPath: String): List<FileMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileMetadataEntity>)

    @Query("DELETE FROM file_metadata WHERE parentPath = :parentPath")
    suspend fun clearFilesForParent(parentPath: String)

    @Query("DELETE FROM file_metadata WHERE path = :path")
    suspend fun deleteFileByPath(path: String)
}
