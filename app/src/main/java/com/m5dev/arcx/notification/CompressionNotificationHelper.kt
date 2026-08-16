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

object CompressionNotificationHelper {

    const val CHANNEL_COMPRESSION_PROGRESS = "channel_compression_progress"
    const val CHANNEL_COMPRESSION_RESULT = "channel_compression_result"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_COMPRESSION_PROGRESS,
                "Archive Compression Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of ongoing archive compressions"
            }

            val resultChannel = NotificationChannel(
                CHANNEL_COMPRESSION_RESULT,
                "Archive Compression Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows notifications when archive creation completes or fails"
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
            "Compressing..."
        }

        return NotificationCompat.Builder(context, CHANNEL_COMPRESSION_PROGRESS)
            .setContentTitle("Compressing $archiveName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
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
            action = ExtractionNotificationHelper.ACTION_OPEN_PATH
            putExtra(ExtractionNotificationHelper.EXTRA_DEST_PATH, destPath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPRESSION_RESULT)
            .setContentTitle("Compression Complete")
            .setContentText("Created $archiveName successfully")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
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
            // Permission might be denied
        }
    }

    fun showErrorNotification(
        context: Context,
        notificationId: Int,
        archiveName: String,
        errorMessage: String
    ) {
        createNotificationChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPRESSION_RESULT)
            .setContentTitle("Compression Failed")
            .setContentText("Failed to create $archiveName: $errorMessage")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission might be denied
        }
    }
}
