package com.example.rrhe

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object ReplicationManager {
    private suspend fun initialDatabaseReplication() {
        try {
            val plants = ApiClient.apiService.getRRHE()
            val nonNullPlants = plants.map { it.ensureNonNullValues() }
            PlantRepository.plantDao.insertPlants(nonNullPlants)
            Log.d("ReplicationManager", "Replicated ${plants.size} plants from API to database at ${System.currentTimeMillis()}")

            val allPlants = PlantRepository.plantDao.getAllPlants()
            withContext(Dispatchers.Main) {
                PlantRepository.plantsMutable.postValue(allPlants)
            }

            PlantRepository.lastSyncTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            PlantRepository.setMainDatabaseConnected(true)
        } catch (e: Exception) {
            Log.e("ReplicationManager", "Exception during initial replication: ${e.message}")
            PlantRepository.setMainDatabaseConnected(false)
        }
    }

    suspend fun initialDatabaseReplicationWithRetry(retryCount: Int = 3, delayMillis: Long = 2000) {
        var attempts = retryCount
        while (attempts > 0) {
            try {
                initialDatabaseReplication()
                if (PlantRepository.isMainDatabaseConnected.value == true) return
            } catch (e: Exception) {
                Log.e("ReplicationManager", "Error during initial replication, retrying... attempts left: $attempts")
                delay(delayMillis)
                if (--attempts == 0) throw e
            }
        }
    }
}