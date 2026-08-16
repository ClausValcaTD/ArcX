package com.m5dev.arcx.data.di

import com.m5dev.arcx.data.repository.FileRepositoryImpl
import com.m5dev.arcx.data.repository.SettingsRepositoryImpl
import com.m5dev.arcx.domain.repository.FileRepository
import com.m5dev.arcx.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        fileRepositoryImpl: FileRepositoryImpl
    ): FileRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
