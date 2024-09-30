package com.example.rrhe

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class MyApplication : Application() {
    lateinit var database: AppDatabase
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        try {
            instance = this
            initializeDatabase()
            initializeWorkManager()
            initializeRepositories()

            // Use the immediate trigger method for testing
            triggerNotificationWorkerImmediately()
        } catch (e: Exception) {
            Log.e("MyApplication", "Error during initialization: ${e.message}", e)
        }
    }

    private fun initializeDatabase() {
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "plant-database"
        )
            .addMigrations(MIGRATION_13_14)
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun initializeWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(InactivityDetector.MyWorkerFactory())
            .build()
        WorkManager.initialize(this, config)
    }

    private fun initializeRepositories() {
        applicationScope.launch(Dispatchers.IO) {
            PlantRepository.initialize(database.plantDao(), database.statsDao())
            StatsRepository.initialize(database.statsDao())
            PlantRepository.checkAndFetchInitialData(applicationContext)
            setupDropdownAdapters(applicationContext)
        }
    }

    private fun scheduleNotificationWorker() {
        // Commented out for testing purposes
        /*
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .addTag("notification_worker")
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        */
    }

    private fun triggerNotificationWorkerImmediately() {
        // Trigger NotificationWorker immediately
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .addTag("notification_worker_test")
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (now.after(scheduledTime)) {
            scheduledTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        return scheduledTime.timeInMillis - now.timeInMillis
    }

    private suspend fun setupDropdownAdapters(context: Context) {
        Log.d("MyApplication", "Setting up dropdown adapters at app launch")
        PlantDropdownAdapter.setupFamilyAdapter(context)
    }

    companion object {
        @get:Synchronized
        lateinit var instance: MyApplication
    }
}
