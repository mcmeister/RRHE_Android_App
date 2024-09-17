package com.example.rrhe

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AppNotificationManager(private val context: Context) {

    companion object {
        const val UPDATE_NOTIFICATION_CHANNEL_ID = "update_notification_channel"
        const val SCHEDULED_NOTIFICATION_CHANNEL_ID = "scheduled_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val updateChannel = NotificationChannel(
                UPDATE_NOTIFICATION_CHANNEL_ID,
                "App Update Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for app updates"
            }

            val scheduledChannel = NotificationChannel(
                SCHEDULED_NOTIFICATION_CHANNEL_ID,
                "Scheduled Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled tasks"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(updateChannel, scheduledChannel))
        }
    }

    @SuppressLint("LaunchActivityFromNotification")
    fun showAppUpdateNotification(title: String, messageBody: String, downloadUrl: String) {
        val intent = Intent(context, DownloadReceiver::class.java).apply {
            putExtra("download_url", downloadUrl)
            putExtra("title", title)
            putExtra("messageBody", messageBody)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notificationBuilder.build())
    }

    fun showScheduledNotification(title: String, messageBody: String, plants: List<Map<String, String>>) {
        // Build the detailed plant information string
        val plantDetails = plants.joinToString(separator = "\n") { plant ->
            val name = plant["NameConcat"].orEmpty()
            val stockId = plant["StockID"].orEmpty()
            val tableName = plant["TableName"].takeIf { !it.isNullOrBlank() } ?: "N/A"
            "• $name (StockID: $stockId, Table: $tableName)"
        }

        val notificationMessage = "$messageBody\n\n$plantDetails"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("search_query", "today")  // Pass "today" as an extra to be handled in MainActivity
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, SCHEDULED_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3, notificationBuilder.build())
    }
}