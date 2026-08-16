package com.m5dev.arcx

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.m5dev.arcx.domain.model.FileItem
import com.m5dev.arcx.domain.model.FileType
import com.m5dev.arcx.domain.model.UserPreferences
import com.m5dev.arcx.domain.repository.FileRepository
import com.m5dev.arcx.domain.repository.SettingsRepository
import com.m5dev.arcx.presentation.ui.filebrowser.FileBrowserViewModel
import com.m5dev.arcx.presentation.ui.filebrowser.StorageLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class FileBrowserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeFileRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var viewModel: FileBrowserViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeFileRepository()
        fakeSettingsRepository = FakeSettingsRepository()
        viewModel = FileBrowserViewModel(fakeRepository, fakeSettingsRepository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLocation_isInternalStorage() {
        val state = viewModel.uiState.value
        assertEquals(StorageLocation.INTERNAL_STORAGE, state.selectedLocation)
        assertEquals("/storage/emulated/0", state.currentPath)
        assertEquals("Internal Storage", state.currentFolderName)
        assertFalse(state.canNavigateUp)
        assertFalse(state.hasStoragePermission)
    }

    @Test
    fun permissionStatusGranted_loadsDirectoryFiles() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasStoragePermission)
        assertFalse(state.isLoading)
        assertEquals(4, state.items.size)
        assertEquals("Documents", state.items[0].name)
    }

    @Test
    fun switchStorageLocation_updatesPathAndLoadsFiles() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onStorageLocationSelected(StorageLocation.DOWNLOADS)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StorageLocation.DOWNLOADS, state.selectedLocation)
        assertEquals("/storage/emulated/0/Download", state.currentPath)
        assertEquals("Downloads", state.currentFolderName)
        assertEquals(1, state.items.size)
        assertEquals("sample.zip", state.items[0].name)
    }

    @Test
    fun folderNavigation_and_navigateUp() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val folder = viewModel.uiState.value.items.first { it.isFolder }
        viewModel.onFolderClick(folder)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals("/storage/emulated/0/Documents", updatedState.currentPath)
        assertEquals("Documents", updatedState.currentFolderName)
        assertTrue(updatedState.canNavigateUp)

        val navigatedBack = viewModel.onNavigateUp()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(navigatedBack)
        val finalState = viewModel.uiState.value
        assertEquals("/storage/emulated/0", finalState.currentPath)
        assertFalse(finalState.canNavigateUp)
    }

    @Test
    fun deleteFile_removesItemAndShowsSnackbar() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val itemToDelete = viewModel.uiState.value.items.first { !it.isFolder && it.extension == "pdf" }
        viewModel.onOptionActionDelete(itemToDelete)

        assertEquals(itemToDelete, viewModel.uiState.value.selectedItemForDelete)

        viewModel.onConfirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedItemForDelete)
        assertNotNull(viewModel.uiState.value.snackbarMessage)
        assertTrue(viewModel.uiState.value.snackbarMessage!!.contains("Deleted"))
    }

    @Test
    fun renameFile_updatesItemAndShowsSnackbar() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val itemToRename = viewModel.uiState.value.items.first { !it.isFolder && it.extension == "pdf" }
        viewModel.onOptionActionRename(itemToRename)

        assertEquals(itemToRename, viewModel.uiState.value.selectedItemForRename)

        viewModel.onConfirmRename("renamed_file.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedItemForRename)
        assertNotNull(viewModel.uiState.value.snackbarMessage)
        assertTrue(viewModel.uiState.value.snackbarMessage!!.contains("Renamed"))
    }

    @Test
    fun extractArchive_success_updatesStateAndShowsSnackbar() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val archiveItem = viewModel.uiState.value.items.first { it.name == "archive.zip" }
        viewModel.onOptionActionExtract(archiveItem)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isExtracting)
        assertNull(state.extractingFileName)
        assertNotNull(state.snackbarMessage)
        assertTrue(state.snackbarMessage!!.contains("Extraction started in background"))
    }

    @Test
    fun extractEncryptedArchive_withoutPassword_triggersPasswordPrompt() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val protectedArchive = viewModel.uiState.value.items.first { it.name == "protected.zip" }
        viewModel.onOptionActionExtract(protectedArchive)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isExtracting)
        assertTrue(state.showPasswordPrompt)
        assertEquals(protectedArchive, state.itemForPasswordExtraction)

        // Submit password
        viewModel.onConfirmPasswordExtraction("secret123")
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.showPasswordPrompt)
        assertFalse(finalState.isExtracting)
        assertNotNull(finalState.snackbarMessage)
        assertTrue(finalState.snackbarMessage!!.contains("Extraction started in background"))
    }

    @Test
    fun compressFile_opensCompressionDialog_and_submitsBackgroundJob() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val docItem = viewModel.uiState.value.items.first { it.name == "document.pdf" }
        viewModel.onOptionActionCompress(docItem)

        val config = viewModel.uiState.value.compressionConfig
        assertNotNull(config)
        assertEquals("document", config!!.defaultName)
        assertEquals(listOf(docItem.path), config.sourcePaths)

        viewModel.onSubmitCompression(
            config = config.copy(format = "ZIP", compressionLevel = "Maximum", password = "pass", encryptionMethod = "AES-256"),
            archiveName = "document"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.compressionConfig)
        assertNotNull(state.snackbarMessage)
        assertTrue(state.snackbarMessage!!.contains("Compression started in background"))
    }

    @Test
    fun compressFile_shortPassword_showsWarningSnackbar() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val docItem = viewModel.uiState.value.items.first { it.name == "document.pdf" }
        viewModel.onOptionActionCompress(docItem)

        val config = viewModel.uiState.value.compressionConfig!!
        viewModel.onSubmitCompression(
            config = config.copy(password = "123"), // < 4 chars
            archiveName = "short_pass"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Password must be at least 4 characters long", state.snackbarMessage)
    }

    @Test
    fun compressFile_existingArchive_triggersOverwritePrompt() = runTest {
        viewModel.onPermissionStatusUpdated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // "archive.zip" already exists in repository
        val docItem = viewModel.uiState.value.items.first { it.name == "document.pdf" }
        viewModel.onOptionActionCompress(docItem)

        val config = viewModel.uiState.value.compressionConfig!!
        viewModel.onSubmitCompression(
            config = config.copy(format = "ZIP"),
            archiveName = "archive" // archive.zip exists
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.compressionConfig)
        assertTrue(state.showOverwritePrompt)
        assertNotNull(state.pendingCompressionConfig)

        // Confirm overwrite
        viewModel.onConfirmOverwriteCompression()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.showOverwritePrompt)
        assertNull(finalState.pendingCompressionConfig)
        assertNotNull(finalState.snackbarMessage)
        assertTrue(finalState.snackbarMessage!!.contains("Compression started in background"))
    }
}

class FakeSettingsRepository : SettingsRepository {
    private val _prefs = MutableStateFlow(UserPreferences())
    override val userPreferencesFlow: Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun updateDefaultExtractLocation(path: String) { _prefs.update { it.copy(defaultExtractLocation = path) } }
    override suspend fun updateDefaultCompressionFormat(format: String) { _prefs.update { it.copy(defaultCompressionFormat = format) } }
    override suspend fun updateDefaultCompressionLevel(level: String) { _prefs.update { it.copy(defaultCompressionLevel = level) } }
    override suspend fun updateAskBeforeOverwrite(ask: Boolean) { _prefs.update { it.copy(askBeforeOverwrite = ask) } }
    override suspend fun updateTheme(theme: String) { _prefs.update { it.copy(theme = theme) } }
    override suspend fun updateDynamicColors(enabled: Boolean) { _prefs.update { it.copy(dynamicColors = enabled) } }
    override suspend fun updateShowHiddenFiles(show: Boolean) { _prefs.update { it.copy(showHiddenFiles = show) } }
    override suspend fun updateShowExtractionNotifications(show: Boolean) { _prefs.update { it.copy(showExtractionNotifications = show) } }
    override suspend fun updateShowCompletionSound(enabled: Boolean) { _prefs.update { it.copy(showCompletionSound = enabled) } }
    override suspend fun updateVibrateOnCompletion(enabled: Boolean) { _prefs.update { it.copy(vibrateOnCompletion = enabled) } }
}

class FakeFileRepository : FileRepository {

    private val filesMap = mutableMapOf<String, MutableList<FileItem>>(
        "/storage/emulated/0" to mutableListOf(
            FileItem(
                id = "/storage/emulated/0/Documents",
                name = "Documents",
                path = "/storage/emulated/0/Documents",
                isFolder = true,
                sizeInBytes = 0,
                formattedSize = "2 items",
                lastModifiedTimestamp = 1000L,
                formattedDate = "Jan 01, 2024",
                extension = "",
                fileType = FileType.FOLDER,
                itemCount = 2
            ),
            FileItem(
                id = "/storage/emulated/0/archive.zip",
                name = "archive.zip",
                path = "/storage/emulated/0/archive.zip",
                isFolder = false,
                sizeInBytes = 1024,
                formattedSize = "1.0 KB",
                lastModifiedTimestamp = 2000L,
                formattedDate = "Jan 02, 2024",
                extension = "zip",
                fileType = FileType.ARCHIVE
            ),
            FileItem(
                id = "/storage/emulated/0/protected.zip",
                name = "protected.zip",
                path = "/storage/emulated/0/protected.zip",
                isFolder = false,
                sizeInBytes = 2048,
                formattedSize = "2.0 KB",
                lastModifiedTimestamp = 2500L,
                formattedDate = "Jan 02, 2024",
                extension = "zip",
                fileType = FileType.ARCHIVE
            ),
            FileItem(
                id = "/storage/emulated/0/document.pdf",
                name = "document.pdf",
                path = "/storage/emulated/0/document.pdf",
                isFolder = false,
                sizeInBytes = 512,
                formattedSize = "512 B",
                lastModifiedTimestamp = 2800L,
                formattedDate = "Jan 02, 2024",
                extension = "pdf",
                fileType = FileType.OTHER
            )
        ),
        "/storage/emulated/0/Documents" to mutableListOf(
            FileItem(
                id = "/storage/emulated/0/Documents/notes.txt",
                name = "notes.txt",
                path = "/storage/emulated/0/Documents/notes.txt",
                isFolder = false,
                sizeInBytes = 500,
                formattedSize = "500 B",
                lastModifiedTimestamp = 3000L,
                formattedDate = "Jan 03, 2024",
                extension = "txt",
                fileType = FileType.OTHER
            )
        ),
        "/storage/emulated/0/Download" to mutableListOf(
            FileItem(
                id = "/storage/emulated/0/Download/sample.zip",
                name = "sample.zip",
                path = "/storage/emulated/0/Download/sample.zip",
                isFolder = false,
                sizeInBytes = 2048,
                formattedSize = "2.0 KB",
                lastModifiedTimestamp = 4000L,
                formattedDate = "Jan 04, 2024",
                extension = "zip",
                fileType = FileType.ARCHIVE
            )
        )
    )

    override suspend fun getFilesForPath(path: String): Result<List<FileItem>> {
        val list = filesMap[path] ?: mutableListOf()
        return Result.success(list)
    }

    override suspend fun deleteFile(path: String): Result<Boolean> {
        filesMap.values.forEach { list ->
            list.removeAll { it.path == path }
        }
        return Result.success(true)
    }

    override suspend fun renameFile(path: String, newName: String): Result<FileItem> {
        for ((_, list) in filesMap) {
            val index = list.indexOfFirst { it.path == path }
            if (index != -1) {
                val old = list[index]
                val updatedPath = old.path.substringBeforeLast('/') + "/" + newName
                val updated = old.copy(
                    id = updatedPath,
                    name = newName,
                    path = updatedPath
                )
                list[index] = updated
                return Result.success(updated)
            }
        }
        return Result.failure(Exception("File not found"))
    }

    override fun getStorageRootPath(): String = "/storage/emulated/0"

    override fun getDownloadsPath(): String = "/storage/emulated/0/Download"

    override suspend fun extractArchive(
        archivePath: String,
        destPath: String,
        password: String?
    ): Result<Boolean> {
        if (archivePath.contains("protected") && password.isNullOrEmpty()) {
            return Result.failure(Exception("Passphrase required for protected archive"))
        }
        return Result.success(true)
    }

    override suspend fun extractArchiveWithProgress(
        archivePath: String,
        destPath: String,
        password: String?,
        onProgress: ((current: Int, total: Int, fileName: String) -> Boolean)?
    ): Result<Boolean> {
        if (archivePath.contains("protected") && password.isNullOrEmpty()) {
            return Result.failure(Exception("Passphrase required for protected archive"))
        }
        onProgress?.invoke(1, 2, "file1.txt")
        onProgress?.invoke(2, 2, "file2.jpg")
        return Result.success(true)
    }

    override suspend fun listArchiveContents(archivePath: String): Result<List<String>> {
        if (archivePath.contains("protected")) {
            return Result.failure(Exception("Passphrase required for protected archive"))
        }
        return Result.success(listOf("file1.txt", "file2.jpg"))
    }

    override suspend fun createArchiveWithProgress(
        sourcePaths: List<String>,
        destArchivePath: String,
        format: String,
        level: String,
        password: String?,
        encryptionMethod: String?,
        onProgress: ((current: Int, total: Int, fileName: String) -> Boolean)?
    ): Result<Boolean> {
        onProgress?.invoke(1, 1, "doc.pdf")
        return Result.success(true)
    }

    override fun getAvailableSpaceInBytes(path: String): Long {
        return 100_000_000L // 100 MB
    }

    override suspend fun calculateTotalSize(paths: List<String>): Long {
        return 1_000_000L // 1 MB
    }
}
