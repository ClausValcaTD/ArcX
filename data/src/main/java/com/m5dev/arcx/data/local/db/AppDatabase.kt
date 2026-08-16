package com.m5dev.arcx.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FileMetadataEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileMetadataDao(): FileMetadataDao
}
