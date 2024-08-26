package com.example.rrhe

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
        applicationScope.launch(Dispatchers.IO) {  // Use IO dispatcher for I/O-bound tasks
            PlantRepository.initialize(database.plantDao(), database.statsDao())
            StatsRepository.initialize(database.statsDao())
            PlantRepository.checkAndFetchInitialData(applicationContext)

            // Setup dropdown adapters
            setupDropdownAdapters(applicationContext)
        }
    }

    private suspend fun setupDropdownAdapters(context: Context) {
        Log.d("MyApplication", "Setting up dropdown adapters at app launch")
        PlantDropdownAdapter.setupFamilyAdapter(context)
    }

    companion object {
        @get:Synchronized
        lateinit var instance: MyApplication
            private set
    }
}
