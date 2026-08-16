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
import com.m5dev.arcx.notification.ExtractionNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File

@HiltWorker
class ExtractionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_ARCHIVE_PATH = "key_archive_path"
        const val KEY_DEST_PATH = "key_dest_path"
        const val KEY_PASSWORD = "key_password"

        const val KEY_CURRENT_FILE = "key_current_file"
        const val KEY_TOTAL_FILES = "key_total_files"
        const val KEY_PERCENTAGE = "key_percentage"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_ARCHIVE_NAME = "key_archive_name"
        const val KEY_ERROR_MESSAGE = "key_error_message"

        const val TAG_EXTRACTION_WORK = "tag_extraction_work"
    }

    override suspend fun doWork(): Result {
        val prefs = try { settingsRepository.userPreferencesFlow.first() } catch (e: Exception) { com.m5dev.arcx.domain.model.UserPreferences() }

        val archivePath = inputData.getString(KEY_ARCHIVE_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing archive path"))
        val destPath = inputData.getString(KEY_DEST_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing destination path"))
        val password = inputData.getString(KEY_PASSWORD)

        val archiveFile = File(archivePath)
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

        val extractResult = fileRepository.extractArchiveWithProgress(
            archivePath = archivePath,
            destPath = destPath,
            password = password
        ) { current, total, fileName ->
            if (isStopped) {
                return@extractArchiveWithProgress false
            }

            val percentage = if (total > 0) (current * 100) / total else 0

            setProgressAsync(
                workDataOf(
                    KEY_ARCHIVE_NAME to archiveName,
                    KEY_CURRENT_FILE to current,
                    KEY_TOTAL_FILES to total,
                    KEY_PERCENTAGE to percentage,
                    KEY_FILE_NAME to fileName,
                    KEY_ARCHIVE_PATH to archivePath,
                    KEY_DEST_PATH to destPath
                )
            )

            if (prefs.showExtractionNotifications) {
                val updatedNotification = ExtractionNotificationHelper.buildProgressNotification(
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

        return extractResult.fold(
            onSuccess = {
                if (prefs.showExtractionNotifications) {
                    ExtractionNotificationHelper.showSuccessNotification(
                        context = context,
                        notificationId = notificationId,
                        archiveName = archiveName,
                        destPath = destPath,
                        showSound = prefs.showCompletionSound,
                        vibrate = prefs.vibrateOnCompletion
                    )
                }
                Result.success(
                    workDataOf(
                        KEY_ARCHIVE_NAME to archiveName,
                        KEY_DEST_PATH to destPath,
                        KEY_ARCHIVE_PATH to archivePath
                    )
                )
            },
            onFailure = { error ->
                val errorMsg = error.localizedMessage ?: "Unknown error"
                if (isStopped) {
                    Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Extraction canceled"))
                } else {
                    if (prefs.showExtractionNotifications) {
                        ExtractionNotificationHelper.showErrorNotification(
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
                            KEY_ARCHIVE_PATH to archivePath
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
        val notification = ExtractionNotificationHelper.buildProgressNotification(
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
