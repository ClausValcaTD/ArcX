package com.m5dev.arcx

import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserViewModel
import com.m5dev.arcx.presentation.ui.filebrowser.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileBrowserViewModelTest {

    private lateinit var viewModel: FileBrowserViewModel

    @Before
    fun setup() {
        viewModel = FileBrowserViewModel()
    }

    @Test
    fun initialLocation_isInternalStorage() {
        val state = viewModel.uiState.value
        assertEquals(StorageLocation.INTERNAL_STORAGE, state.selectedLocation)
        assertEquals("/storage/emulated/0", state.currentPath)
        assertEquals("Internal Storage", state.currentFolderName)
        assertFalse(state.canNavigateUp)
        assertTrue(state.items.isNotEmpty())
    }

    @Test
    fun switchStorageLocation_updatesState() {
        viewModel.onStorageLocationSelected(StorageLocation.DOWNLOADS)
        val state = viewModel.uiState.value
        assertEquals(StorageLocation.DOWNLOADS, state.selectedLocation)
        assertEquals("/storage/emulated/0/Download", state.currentPath)
        assertEquals("Downloads", state.currentFolderName)
        assertFalse(state.canNavigateUp)
        assertTrue(state.items.isNotEmpty())
    }

    @Test
    fun folderNavigation_and_navigateUp() {
        val initialItems = viewModel.uiState.value.items
        val folder = initialItems.first { it.isFolder }

        viewModel.onFolderClick(folder)

        val updatedState = viewModel.uiState.value
        assertEquals(folder.path, updatedState.currentPath)
        assertEquals(folder.name, updatedState.currentFolderName)
        assertTrue(updatedState.canNavigateUp)

        val navigatedBack = viewModel.onNavigateUp()
        assertTrue(navigatedBack)

        val finalState = viewModel.uiState.value
        assertEquals("/storage/emulated/0", finalState.currentPath)
        assertFalse(finalState.canNavigateUp)
    }
}
