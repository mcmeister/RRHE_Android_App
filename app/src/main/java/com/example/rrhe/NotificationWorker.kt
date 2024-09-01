package com.example.rrhe

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val tag = "NotificationWorker"

    override fun doWork(): Result {
        Log.d(tag, "doWork: Starting notification worker")

        val todayDate = getCurrentDate()
        Log.d(tag, "doWork: Today's date is $todayDate")

        val plantDao = MyApplication.instance.database.plantDao()
        val plantsReadyForHarvest = plantDao.getPlantsReadyForNotification(todayDate)

        Log.d(tag, "doWork: Found ${plantsReadyForHarvest?.size ?: 0} plants ready for harvest")

        if (plantsReadyForHarvest.isNullOrEmpty()) {
            Log.d(tag, "doWork: No plants to notify.")
            return Result.success()
        }

        // Build a list of maps with NameConcat, TableName, and StockID for each plant
        val plantList = plantsReadyForHarvest.map { plant ->
            Log.d(tag, "doWork: Processing plant with StockID ${plant?.StockID}, NameConcat ${plant?.NameConcat}, TableName ${plant?.TableName}")
            mapOf(
                "StockID" to plant?.StockID.toString(),
                "NameConcat" to plant?.NameConcat.orEmpty(),
                "TableName" to plant?.TableName.orEmpty()
            )
        }

        // Create a dynamic title and message based on the number of plants
        val title = if (plantsReadyForHarvest.size == 1) {
            "Harvest ${plantsReadyForHarvest.first()?.NameConcat} Today"
        } else {
            "Harvest ${plantsReadyForHarvest.size} Plants Today"
        }

        val message = if (plantsReadyForHarvest.size == 1) {
            "The plant ${plantsReadyForHarvest.first()?.NameConcat} is ready for harvest today."
        } else {
            "The following plants are ready for harvest today:"
        }

        Log.d(tag, "doWork: Showing notification with title: $title and message: $message")

        showNotification(title, message, plantList)

        Log.d(tag, "doWork: Notification worker completed")
        return Result.success()
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showNotification(title: String, message: String, plants: List<Map<String, String>>) {
        Log.d(tag, "showNotification: Preparing to show notification with title: $title and message: $message")

        val appNotificationManager = AppNotificationManager(applicationContext)
        appNotificationManager.showScheduledNotification(title, message, plants)

        Log.d(tag, "showNotification: Notification displayed successfully")
    }
}
