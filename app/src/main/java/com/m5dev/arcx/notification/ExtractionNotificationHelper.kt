package com.m5dev.arcx.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.m5dev.arcx.MainActivity

object ExtractionNotificationHelper {

    const val CHANNEL_EXTRACTION_PROGRESS = "channel_extraction_progress"
    const val CHANNEL_EXTRACTION_RESULT = "channel_extraction_result"

    const val ACTION_CANCEL_EXTRACTION = "com.m5dev.arcx.ACTION_CANCEL_EXTRACTION"
    const val ACTION_OPEN_PATH = "com.m5dev.arcx.ACTION_OPEN_PATH"

    const val EXTRA_WORK_ID = "extra_work_id"
    const val EXTRA_DEST_PATH = "extra_dest_path"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_EXTRACTION_PROGRESS,
                "Archive Extraction Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of ongoing archive extractions"
            }

            val resultChannel = NotificationChannel(
                CHANNEL_EXTRACTION_RESULT,
                "Archive Extraction Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows notifications when extraction completes or fails"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(resultChannel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        workIdString: String,
        archiveName: String,
        currentFile: Int,
        totalFiles: Int,
        percentage: Int,
        cancelPendingIntent: PendingIntent
    ): Notification {
        createNotificationChannels(context)

        val contentText = if (totalFiles > 0) {
            "$currentFile/$totalFiles files ($percentage%)"
        } else {
            "Extracting..."
        }

        return NotificationCompat.Builder(context, CHANNEL_EXTRACTION_PROGRESS)
            .setContentTitle("Extracting $archiveName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(if (totalFiles > 0) totalFiles else 100, if (totalFiles > 0) currentFile else percentage, totalFiles <= 0)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showSuccessNotification(
        context: Context,
        notificationId: Int,
        archiveName: String,
        destPath: String
    ) {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_PATH
            putExtra(EXTRA_DEST_PATH, destPath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EXTRACTION_RESULT)
            .setContentTitle("Extraction Complete")
            .setContentText("Extracted $archiveName successfully")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open folder",
                openPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission might be denied on Android 13+ if user didn't grant POST_NOTIFICATIONS
        }
    }

    fun showErrorNotification(
        context: Context,
        notificationId: Int,
        archiveName: String,
        errorMessage: String
    ) {
        createNotificationChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_EXTRACTION_RESULT)
            .setContentTitle("Extraction Failed")
            .setContentText("Failed to extract $archiveName: $errorMessage")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission might be denied on Android 13+
        }
    }
}
