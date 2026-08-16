package com.m5dev.arcx.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.m5dev.arcx.domain.repository.FileRepository
import com.m5dev.arcx.domain.repository.SettingsRepository
import com.m5dev.arcx.notification.CompressionNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File

@HiltWorker
class CompressionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SOURCE_PATHS = "key_source_paths"
        const val KEY_DEST_ARCHIVE_PATH = "key_dest_archive_path"
        const val KEY_FORMAT = "key_format"
        const val KEY_LEVEL = "key_level"
        const val KEY_PASSWORD = "key_password"
        const val KEY_ENCRYPTION_METHOD = "key_encryption_method"

        const val KEY_CURRENT_FILE = "key_current_file"
        const val KEY_TOTAL_FILES = "key_total_files"
        const val KEY_PERCENTAGE = "key_percentage"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_ARCHIVE_NAME = "key_archive_name"
        const val KEY_ERROR_MESSAGE = "key_error_message"

        const val TAG_COMPRESSION_WORK = "tag_compression_work"
    }

    override suspend fun doWork(): Result {
        val prefs = try { settingsRepository.userPreferencesFlow.first() } catch (e: Exception) { com.m5dev.arcx.domain.model.UserPreferences() }

        val sourcePaths = inputData.getStringArray(KEY_SOURCE_PATHS)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing source paths"))
        val destArchivePath = inputData.getString(KEY_DEST_ARCHIVE_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing destination archive path"))
        val format = inputData.getString(KEY_FORMAT) ?: "zip"
        val level = inputData.getString(KEY_LEVEL) ?: "Normal"
        val password = inputData.getString(KEY_PASSWORD)
        val encryptionMethod = inputData.getString(KEY_ENCRYPTION_METHOD)

        val archiveFile = File(destArchivePath)
        val archiveName = archiveFile.name
        val notificationId = id.hashCode()

        val cancelPendingIntent = WorkManager.getInstance(context)
            .createCancelPendingIntent(id)

        val initialForegroundInfo = createForegroundInfo(
            archiveName = archiveName,
            current = 0,
            total = 0,
            percentage = 0,
            notificationId = notificationId,
            cancelPendingIntent = cancelPendingIntent
        )
        setForeground(initialForegroundInfo)

        val compressResult = fileRepository.createArchiveWithProgress(
            sourcePaths = sourcePaths.toList(),
            destArchivePath = destArchivePath,
            format = format,
            level = level,
            password = password,
            encryptionMethod = encryptionMethod
        ) { current, total, fileName ->
            if (isStopped) {
                return@createArchiveWithProgress false
            }

            val percentage = if (total > 0) (current * 100) / total else 0

            setProgressAsync(
                workDataOf(
                    KEY_ARCHIVE_NAME to archiveName,
                    KEY_CURRENT_FILE to current,
                    KEY_TOTAL_FILES to total,
                    KEY_PERCENTAGE to percentage,
                    KEY_FILE_NAME to fileName,
                    KEY_DEST_ARCHIVE_PATH to destArchivePath
                )
            )

            if (prefs.showExtractionNotifications) {
                val updatedNotification = CompressionNotificationHelper.buildProgressNotification(
                    context = context,
                    workIdString = id.toString(),
                    archiveName = archiveName,
                    currentFile = current,
                    totalFiles = total,
                    percentage = percentage,
                    cancelPendingIntent = cancelPendingIntent
                )

                try {
                    androidx.core.app.NotificationManagerCompat.from(context)
                        .notify(notificationId, updatedNotification)
                } catch (e: SecurityException) {
                    // Ignore if notification permission is revoked
                }
            }

            !isStopped
        }

        return compressResult.fold(
            onSuccess = {
                val parentPath = archiveFile.parentFile?.absolutePath ?: destArchivePath
                if (prefs.showExtractionNotifications) {
                    CompressionNotificationHelper.showSuccessNotification(
                        context = context,
                        notificationId = notificationId,
                        archiveName = archiveName,
                        destPath = parentPath,
                        showSound = prefs.showCompletionSound,
                        vibrate = prefs.vibrateOnCompletion
                    )
                }
                Result.success(
                    workDataOf(
                        KEY_ARCHIVE_NAME to archiveName,
                        KEY_DEST_ARCHIVE_PATH to destArchivePath
                    )
                )
            },
            onFailure = { error ->
                val errorMsg = error.localizedMessage ?: "Unknown error"
                if (isStopped) {
                    Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Compression canceled"))
                } else {
                    if (prefs.showExtractionNotifications) {
                        CompressionNotificationHelper.showErrorNotification(
                            context = context,
                            notificationId = notificationId,
                            archiveName = archiveName,
                            errorMessage = errorMsg
                        )
                    }
                    Result.failure(
                        workDataOf(
                            KEY_ARCHIVE_NAME to archiveName,
                            KEY_ERROR_MESSAGE to errorMsg,
                            KEY_DEST_ARCHIVE_PATH to destArchivePath
                        )
                    )
                }
            }
        )
    }

    private fun createForegroundInfo(
        archiveName: String,
        current: Int,
        total: Int,
        percentage: Int,
        notificationId: Int,
        cancelPendingIntent: android.app.PendingIntent
    ): ForegroundInfo {
        val notification = CompressionNotificationHelper.buildProgressNotification(
            context = context,
            workIdString = id.toString(),
            archiveName = archiveName,
            currentFile = current,
            totalFiles = total,
            percentage = percentage,
            cancelPendingIntent = cancelPendingIntent
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
