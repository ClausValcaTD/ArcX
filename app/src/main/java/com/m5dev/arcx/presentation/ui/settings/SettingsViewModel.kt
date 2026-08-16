package com.m5dev.arcx.presentation.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m5dev.arcx.domain.model.FileItem
import com.m5dev.arcx.domain.model.UserPreferences
import com.m5dev.arcx.domain.repository.FileRepository
import com.m5dev.arcx.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val showThemeDialog: Boolean = false,
    val showFormatDialog: Boolean = false,
    val showLevelDialog: Boolean = false,
    val showFolderPickerDialog: Boolean = false,
    val folderPickerCurrentPath: String = "",
    val folderPickerItems: List<FileItem> = emptyList(),
    val showLicensesDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(
        DialogState()
    )

    data class DialogState(
        val showThemeDialog: Boolean = false,
        val showFormatDialog: Boolean = false,
        val showLevelDialog: Boolean = false,
        val showFolderPickerDialog: Boolean = false,
        val folderPickerCurrentPath: String = "",
        val folderPickerItems: List<FileItem> = emptyList(),
        val showLicensesDialog: Boolean = false,
        val showPrivacyDialog: Boolean = false,
        val snackbarMessage: String? = null
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userPreferencesFlow,
        _dialogState
    ) { prefs, dialogs ->
        SettingsUiState(
            preferences = prefs,
            showThemeDialog = dialogs.showThemeDialog,
            showFormatDialog = dialogs.showFormatDialog,
            showLevelDialog = dialogs.showLevelDialog,
            showFolderPickerDialog = dialogs.showFolderPickerDialog,
            folderPickerCurrentPath = dialogs.folderPickerCurrentPath,
            folderPickerItems = dialogs.folderPickerItems,
            showLicensesDialog = dialogs.showLicensesDialog,
            showPrivacyDialog = dialogs.showPrivacyDialog,
            snackbarMessage = dialogs.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState()
    )

    // General Settings Actions
    fun onOpenFolderPicker() {
        val rootPath = fileRepository.getStorageRootPath()
        loadFolderPickerDirectory(rootPath)
        _dialogState.update {
            it.copy(
                showFolderPickerDialog = true,
                folderPickerCurrentPath = rootPath
            )
        }
    }

    fun onFolderPickerNavigateTo(path: String) {
        loadFolderPickerDirectory(path)
        _dialogState.update { it.copy(folderPickerCurrentPath = path) }
    }

    fun onFolderPickerNavigateUp() {
        val current = _dialogState.value.folderPickerCurrentPath
        val parent = File(current).parent
        if (parent != null && File(parent).exists()) {
            loadFolderPickerDirectory(parent)
            _dialogState.update { it.copy(folderPickerCurrentPath = parent) }
        }
    }

    fun onSelectFolderPickerDestination(path: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultExtractLocation(path)
            _dialogState.update {
                it.copy(
                    showFolderPickerDialog = false,
                    snackbarMessage = "Default extract location updated"
                )
            }
        }
    }

    fun onResetFolderPickerDestinationToDefault() {
        viewModelScope.launch {
            settingsRepository.updateDefaultExtractLocation("")
            _dialogState.update {
                it.copy(
                    showFolderPickerDialog = false,
                    snackbarMessage = "Extract location reset to default (Source directory)"
                )
            }
        }
    }

    fun onDismissFolderPicker() {
        _dialogState.update { it.copy(showFolderPickerDialog = false) }
    }

    private fun loadFolderPickerDirectory(path: String) {
        viewModelScope.launch {
            val result = fileRepository.getFilesForPath(path)
            result.fold(
                onSuccess = { items ->
                    // Only show directories in folder picker
                    val folders = items.filter { it.isFolder }
                    _dialogState.update { it.copy(folderPickerItems = folders) }
                },
                onFailure = {
                    _dialogState.update { it.copy(folderPickerItems = emptyList()) }
                }
            )
        }
    }

    fun onFormatClick() {
        _dialogState.update { it.copy(showFormatDialog = true) }
    }

    fun onSelectFormat(format: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultCompressionFormat(format)
            _dialogState.update { it.copy(showFormatDialog = false) }
        }
    }

    fun onDismissFormatDialog() {
        _dialogState.update { it.copy(showFormatDialog = false) }
    }

    fun onLevelClick() {
        _dialogState.update { it.copy(showLevelDialog = true) }
    }

    fun onSelectLevel(level: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultCompressionLevel(level)
            _dialogState.update { it.copy(showLevelDialog = false) }
        }
    }

    fun onDismissLevelDialog() {
        _dialogState.update { it.copy(showLevelDialog = false) }
    }

    fun onAskBeforeOverwriteToggle(checked: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAskBeforeOverwrite(checked)
        }
    }

    // Appearance Actions
    fun onThemeClick() {
        _dialogState.update { it.copy(showThemeDialog = true) }
    }

    fun onSelectTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
            _dialogState.update { it.copy(showThemeDialog = false) }
        }
    }

    fun onDismissThemeDialog() {
        _dialogState.update { it.copy(showThemeDialog = false) }
    }

    fun onDynamicColorsToggle(checked: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDynamicColors(checked)
        }
    }

    fun onShowHiddenFilesToggle(checked: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowHiddenFiles(checked)
        }
    }

    // Notifications Actions
    fun onShowExtractionNotificationsToggle(checked: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowExtractionNotifications(checked)
        }
    }

    fun onShowCompletionSoundToggle(checked: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowCompletionSound(checked)
        }
    }

    fun onVibrateOnCompletionToggle(checked: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateVibrateOnCompletion(checked)
        }
    }

    // About Actions
    fun onLicensesClick() {
        _dialogState.update { it.copy(showLicensesDialog = true) }
    }

    fun onDismissLicensesDialog() {
        _dialogState.update { it.copy(showLicensesDialog = false) }
    }

    fun onPrivacyPolicyClick() {
        _dialogState.update { it.copy(showPrivacyDialog = true) }
    }

    fun onDismissPrivacyDialog() {
        _dialogState.update { it.copy(showPrivacyDialog = false) }
    }

    fun onGitHubRepoClick(context: Context) {
        openUrl(context, "https://github.com/m5dev/arcx")
    }

    fun onRateAppClick(context: Context) {
        val appPackageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            openUrl(context, "https://play.google.com/store/apps/details?id=$appPackageName")
        }
    }

    fun onSnackbarDismissed() {
        _dialogState.update { it.copy(snackbarMessage = null) }
    }

    private fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _dialogState.update { it.copy(snackbarMessage = "Unable to open browser") }
        }
    }
}
