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
        const val PLANT_NOTIFICATION_CHANNEL_ID = "plant_notification_channel"
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
            )
            updateChannel.description = "Notifications for app updates"

            val plantChannel = NotificationChannel(
                PLANT_NOTIFICATION_CHANNEL_ID,
                "Plant Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            plantChannel.description = "Notifications for plant-related updates"

            val scheduledChannel = NotificationChannel(
                SCHEDULED_NOTIFICATION_CHANNEL_ID,
                "Scheduled Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            scheduledChannel.description = "Notifications for scheduled tasks"

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(updateChannel, plantChannel, scheduledChannel))
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

    fun showPlantNotification(title: String, messageBody: String, stockId: String, nameConcat: String) {
        val intent = Intent(context, PlantDetailsActivity::class.java).apply {
            putExtra("StockID", stockId)
            putExtra("NameConcat", nameConcat)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, PLANT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notificationBuilder.build())
    }

    fun showScheduledNotification(title: String, messageBody: String, data: Map<String, String>) {
        val intent = Intent(context, PlantDetailsActivity::class.java).apply {
            putExtra("StockID", data["StockID"])
            putExtra("NameConcat", data["NameConcat"])
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3, notificationBuilder.build())
    }
}
