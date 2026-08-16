package com.m5dev.arcx.domain.repository

import com.m5dev.arcx.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun updateDefaultExtractLocation(path: String)
    suspend fun updateDefaultCompressionFormat(format: String)
    suspend fun updateDefaultCompressionLevel(level: String)
    suspend fun updateAskBeforeOverwrite(ask: Boolean)

    suspend fun updateTheme(theme: String)
    suspend fun updateDynamicColors(enabled: Boolean)
    suspend fun updateShowHiddenFiles(show: Boolean)

    suspend fun updateShowExtractionNotifications(show: Boolean)
    suspend fun updateShowCompletionSound(enabled: Boolean)
    suspend fun updateVibrateOnCompletion(enabled: Boolean)
}
