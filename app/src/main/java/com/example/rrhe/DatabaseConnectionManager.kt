package com.example.rrhe

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.pow

object DatabaseConnectionManager {

    private val mutex = Mutex()
    private var failureCount = 0
    private const val MAX_FAILURES = 5
    private const val INITIAL_DELAY_MILLIS = 2000L

    suspend fun checkMainDatabaseConnection(): Boolean {
        return mutex.withLock {
            if (PlantRepository.isMainDatabaseConnected.value == true) {
                Log.d("DatabaseConnectionManager", "Already connected to the main database.")
                return@withLock true
            }

            try {
                val isConnected = ApiClient.apiService.getRRHE().isNotEmpty()
                PlantRepository.setMainDatabaseConnected(isConnected)
                if (isConnected) {
                    Log.d("DatabaseConnectionManager", "Successfully connected to the main database.")
                    failureCount = 0
                } else {
                    handleConnectionFailure()
                }
                return@withLock isConnected
            } catch (e: Exception) {
                Log.e("DatabaseConnectionManager", "Error checking main database connection: ${e.message}")
                handleConnectionFailure()
                return@withLock false
            }
        }
    }

    private suspend fun handleConnectionFailure() {
        failureCount++
        PlantRepository.setMainDatabaseConnected(false)
        if (failureCount >= MAX_FAILURES) {
            Log.e("DatabaseConnectionManager", "Max failure attempts reached. Implementing backoff strategy.")
            val backoffDelay = INITIAL_DELAY_MILLIS * (2.0.pow(failureCount - MAX_FAILURES)).toLong()
            Log.d("DatabaseConnectionManager", "Delaying next connection attempt by ${backoffDelay}ms.")
            delay(backoffDelay)
        }
    }

    suspend fun checkMainDatabaseConnectionWithRetry(retryCount: Int = 3, initialDelayMillis: Long = 2000L): Boolean {
        var currentDelay = initialDelayMillis

        repeat(retryCount) { attempt ->
            val isConnected = checkMainDatabaseConnection()
            if (isConnected) {
                return true
            } else {
                Log.e(
                    "DatabaseConnectionManager",
                    "No connection to main database. Retrying... Attempts left: ${retryCount - attempt - 1}"
                )
                delay(currentDelay)
                currentDelay *= 2 // Exponential backoff
            }
        }

        Log.e("DatabaseConnectionManager", "Failed to connect to main database after $retryCount attempts.")
        return false
    }
}
