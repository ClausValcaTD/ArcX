package com.m5dev.arcx.data.di

import android.content.Context
import androidx.room.Room
import com.m5dev.arcx.data.local.db.AppDatabase
import com.m5dev.arcx.data.local.db.FileMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "arcx_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideFileMetadataDao(database: AppDatabase): FileMetadataDao {
        return database.fileMetadataDao()
    }
}
