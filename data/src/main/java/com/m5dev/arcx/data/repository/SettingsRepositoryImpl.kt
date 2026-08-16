package com.m5dev.arcx.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.m5dev.arcx.domain.model.UserPreferences
import com.m5dev.arcx.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val DEFAULT_EXTRACT_LOCATION = stringPreferencesKey("default_extract_location")
        val DEFAULT_COMPRESSION_FORMAT = stringPreferencesKey("default_compression_format")
        val DEFAULT_COMPRESSION_LEVEL = stringPreferencesKey("default_compression_level")
        val ASK_BEFORE_OVERWRITE = booleanPreferencesKey("ask_before_overwrite")

        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")

        val SHOW_EXTRACTION_NOTIFICATIONS = booleanPreferencesKey("show_extraction_notifications")
        val SHOW_COMPLETION_SOUND = booleanPreferencesKey("show_completion_sound")
        val VIBRATE_ON_COMPLETION = booleanPreferencesKey("vibrate_on_completion")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapUserPreferences(preferences)
        }

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        val defaultExtractLocation = preferences[PreferencesKeys.DEFAULT_EXTRACT_LOCATION] ?: ""
        val defaultCompressionFormat = preferences[PreferencesKeys.DEFAULT_COMPRESSION_FORMAT] ?: "ZIP"
        val defaultCompressionLevel = preferences[PreferencesKeys.DEFAULT_COMPRESSION_LEVEL] ?: "Normal"
        val askBeforeOverwrite = preferences[PreferencesKeys.ASK_BEFORE_OVERWRITE] ?: true

        val theme = preferences[PreferencesKeys.THEME] ?: "SYSTEM"
        val dynamicColors = preferences[PreferencesKeys.DYNAMIC_COLORS] ?: true
        val showHiddenFiles = preferences[PreferencesKeys.SHOW_HIDDEN_FILES] ?: false

        val showExtractionNotifications = preferences[PreferencesKeys.SHOW_EXTRACTION_NOTIFICATIONS] ?: true
        val showCompletionSound = preferences[PreferencesKeys.SHOW_COMPLETION_SOUND] ?: true
        val vibrateOnCompletion = preferences[PreferencesKeys.VIBRATE_ON_COMPLETION] ?: true

        return UserPreferences(
            defaultExtractLocation = defaultExtractLocation,
            defaultCompressionFormat = defaultCompressionFormat,
            defaultCompressionLevel = defaultCompressionLevel,
            askBeforeOverwrite = askBeforeOverwrite,
            theme = theme,
            dynamicColors = dynamicColors,
            showHiddenFiles = showHiddenFiles,
            showExtractionNotifications = showExtractionNotifications,
            showCompletionSound = showCompletionSound,
            vibrateOnCompletion = vibrateOnCompletion
        )
    }

    override suspend fun updateDefaultExtractLocation(path: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_EXTRACT_LOCATION] = path
        }
    }

    override suspend fun updateDefaultCompressionFormat(format: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_COMPRESSION_FORMAT] = format
        }
    }

    override suspend fun updateDefaultCompressionLevel(level: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_COMPRESSION_LEVEL] = level
        }
    }

    override suspend fun updateAskBeforeOverwrite(ask: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASK_BEFORE_OVERWRITE] = ask
        }
    }

    override suspend fun updateTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    override suspend fun updateDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLORS] = enabled
        }
    }

    override suspend fun updateShowHiddenFiles(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HIDDEN_FILES] = show
        }
    }

    override suspend fun updateShowExtractionNotifications(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_EXTRACTION_NOTIFICATIONS] = show
        }
    }

    override suspend fun updateShowCompletionSound(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_COMPLETION_SOUND] = enabled
        }
    }

    override suspend fun updateVibrateOnCompletion(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATE_ON_COMPLETION] = enabled
        }
    }
}
