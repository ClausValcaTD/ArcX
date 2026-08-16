package com.m5dev.arcx

import com.m5dev.arcx.presentation.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeFileRepository: FakeFileRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
        fakeFileRepository = FakeFileRepository()
        viewModel = SettingsViewModel(fakeSettingsRepository, fakeFileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialPreferences_matchesDefaults() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.preferences.defaultExtractLocation)
        assertEquals("ZIP", state.preferences.defaultCompressionFormat)
        assertEquals("Normal", state.preferences.defaultCompressionLevel)
        assertTrue(state.preferences.askBeforeOverwrite)
        assertEquals("SYSTEM", state.preferences.theme)
        assertTrue(state.preferences.dynamicColors)
        assertFalse(state.preferences.showHiddenFiles)
        assertTrue(state.preferences.showExtractionNotifications)
        assertTrue(state.preferences.showCompletionSound)
        assertTrue(state.preferences.vibrateOnCompletion)
    }

    @Test
    fun updateTheme_updatesUiStateAndRepository() = runTest {
        viewModel.onSelectTheme("DARK")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("DARK", state.preferences.theme)
        assertFalse(state.showThemeDialog)
    }

    @Test
    fun updateCompressionFormat_and_Level() = runTest {
        viewModel.onSelectFormat("7Z")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("7Z", viewModel.uiState.value.preferences.defaultCompressionFormat)

        viewModel.onSelectLevel("Maximum")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Maximum", viewModel.uiState.value.preferences.defaultCompressionLevel)
    }

    @Test
    fun updateFolderPickerDestination_and_resetToDefault() = runTest {
        val newLocation = "/storage/emulated/0/Extracted"
        viewModel.onSelectFolderPickerDestination(newLocation)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(newLocation, state.preferences.defaultExtractLocation)
        assertFalse(state.showFolderPickerDialog)
        assertNotNull(state.snackbarMessage)

        viewModel.onResetFolderPickerDestinationToDefault()
        testDispatcher.scheduler.advanceUntilIdle()

        val resetState = viewModel.uiState.value
        assertEquals("", resetState.preferences.defaultExtractLocation)
        assertNotNull(resetState.snackbarMessage)
    }

    @Test
    fun toggleBooleanPreferences_updatesUiState() = runTest {
        viewModel.onAskBeforeOverwriteToggle(false)
        viewModel.onDynamicColorsToggle(false)
        viewModel.onShowHiddenFilesToggle(true)
        viewModel.onShowExtractionNotificationsToggle(false)
        viewModel.onShowCompletionSoundToggle(false)
        viewModel.onVibrateOnCompletionToggle(false)

        testDispatcher.scheduler.advanceUntilIdle()

        val prefs = viewModel.uiState.value.preferences
        assertFalse(prefs.askBeforeOverwrite)
        assertFalse(prefs.dynamicColors)
        assertTrue(prefs.showHiddenFiles)
        assertFalse(prefs.showExtractionNotifications)
        assertFalse(prefs.showCompletionSound)
        assertFalse(prefs.vibrateOnCompletion)
    }

    @Test
    fun dialogStateToggles_licenses_and_privacy() = runTest {
        viewModel.onLicensesClick()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showLicensesDialog)

        viewModel.onDismissLicensesDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showLicensesDialog)

        viewModel.onPrivacyPolicyClick()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showPrivacyDialog)

        viewModel.onDismissPrivacyDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showPrivacyDialog)
    }
}
