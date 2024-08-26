package com.example.rrhe

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val tag = "NotificationWorker"

    override fun doWork(): Result {
        Log.d(tag, "doWork: Starting notification worker")

        // Query the database for plants with today's PlantedEnd date
        val todayDate = getCurrentDate()
        Log.d(tag, "doWork: Today's date is $todayDate")

        val plantsReadyForHarvest = MyApplication.instance.database.plantDao().getPlantsReadyForNotification(todayDate)
        Log.d(tag, "doWork: Found ${plantsReadyForHarvest?.size ?: 0} plants ready for harvest")

        // Iterate over the plants and send notifications
        plantsReadyForHarvest?.forEach { plant ->
            Log.d(tag, "doWork: Processing plant with StockID ${plant?.StockID} and NameConcat ${plant?.NameConcat}")

            // Prepare data for notification
            val data = Data.Builder()
                .putString("StockID", plant?.StockID.toString())
                .putString("NameConcat", plant?.NameConcat)
                .build()

            Log.d(tag, "doWork: Notification data prepared for plant ${plant?.NameConcat}")

            // Show the notification with a dynamic title
            val title = "Harvest ${plant?.NameConcat} Today"
            val message = "Check details for plant ${plant?.NameConcat}"
            Log.d(tag, "doWork: Showing notification with title: $title and message: $message")

            showNotification(title, message, data)
        }

        Log.d(tag, "doWork: Notification worker completed")
        return Result.success()
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showNotification(title: String, message: String, data: Data) {
        Log.d(tag, "showNotification: Preparing to show notification with title: $title and message: $message")

        val appNotificationManager = AppNotificationManager(applicationContext)
        val notificationData = mapOf(
            "StockID" to data.getString("StockID").orEmpty(),
            "NameConcat" to data.getString("NameConcat").orEmpty()
        )

        Log.d(tag, "showNotification: Notification data map created with StockID: ${notificationData["StockID"]} and NameConcat: ${notificationData["NameConcat"]}")

        appNotificationManager.showScheduledNotification(title, message, notificationData)

        Log.d(tag, "showNotification: Notification displayed successfully")
    }
}
