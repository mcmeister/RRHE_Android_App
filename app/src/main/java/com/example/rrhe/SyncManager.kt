package com.example.rrhe

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SyncManager {
    private var initialToastShown = false
    private var shouldStopSyncing = false
    private var connectionMessageLogged = false

    private var lastConnectionCheckTime: Long = 0
    private const val CONNECTION_CHECK_INTERVAL = 600000L // 10 minutes between checks

    private suspend fun syncChanges(): Boolean {
        if (PlantRepository.isSyncing || shouldStopSyncing) return false
        PlantRepository.isSyncing = true

        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastConnectionCheckTime > CONNECTION_CHECK_INTERVAL) {
                    if (!DatabaseConnectionManager.checkMainDatabaseConnection()) {
                        if (!connectionMessageLogged) {
                            Log.e("SyncManager", "No connection to main database; skipping sync")
                            connectionMessageLogged = true
                        }
                        PlantRepository.isSyncing = false
                        return@withContext false
                    }
                    lastConnectionCheckTime = currentTime
                } else if (PlantRepository.isMainDatabaseConnected.value != true) {
                    if (!connectionMessageLogged) {
                        Log.e("SyncManager", "No connection to main database; skipping sync")
                        connectionMessageLogged = true
                    }
                    PlantRepository.isSyncing = false
                    return@withContext false
                }

                Log.d("SyncManager", "Connected to main database successfully.")
                connectionMessageLogged = false // Reset message log status on successful connection

                val successfullySynced = mutableListOf<PlantUpdateRequest>()
                for (updateRequest in PlantRepository.unsyncedChanges) {
                    if (shouldStopSyncing) break
                    val updateResponse = try {
                        ApiClient.apiService.updatePlant(updateRequest)
                    } catch (e: HttpException) {
                        null
                    }

                    if (updateResponse != null) {
                        successfullySynced.add(updateRequest)
                        Log.e("SyncManager", "POST to main database successful with ID: ${updateRequest.StockID}")
                    } else {
                        Log.e("SyncManager", "Sync changes failed for plant with ID: ${updateRequest.StockID}")
                    }
                }
                PlantRepository.unsyncedChanges.removeAll(successfullySynced)

                // Fetch and merge the latest changes from the main database
                val changes = try {
                    fetchRRHEChangesWithRetry()
                } catch (e: HttpException) {
                    Log.e("SyncManager", "Fetching changes failed with exception: ${e.message}")
                    null
                }

                changes?.let {
                    if (it.isNotEmpty() && !shouldStopSyncing) {
                        val nonNullChanges = it.map { plant -> plant.ensureNonNullValues() }
                        mergeChanges(nonNullChanges)
                        PlantRepository.updateLocalPlants(nonNullChanges)
                        PlantRepository.lastSyncTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        withContext(Dispatchers.Main) {
                            if (!initialToastShown) {
                                showToast(
                                    MyApplication.instance,
                                    "Plant list merged from main database"
                                )
                                initialToastShown = true
                            }
                        }
                    }
                }

                // Refresh the local plant list to ensure it's up to date with the main database
                refreshLocalPlantList()

                true
            } catch (e: Exception) {
                Log.e("SyncManager", "An unexpected error occurred during sync: ${e.message}", e)
                false
            } finally {
                PlantRepository.isSyncing = false
            }
        }
    }

    suspend fun syncNewPlant(newPlant: Plant, onStockIDFetched: (Int) -> Unit) {
        val job = SupervisorJob()
        withContext(Dispatchers.IO + job) {
            try {
                // Step 1: Insert the new plant into the main database
                val insertResponse = ApiClient.apiService.insertNewPlant(newPlant)

                insertResponse.let { response ->
                    Log.d("SyncManager", "INSERT to main database successful with new StockID: ${response.StockID}")

                    // Step 2: Fetch the newly inserted plant from the main database (ensure it has correct StockID)
                    val fetchedPlant = PlantRepository.getPlantByStockID(response.StockID!!)

                    fetchedPlant?.let {
                        // Step 3: Save the fetched plant with correct StockID to the local database
                        PlantRepository.saveNewPlantLocally(MyApplication.instance, it)

                        // Step 4: Delete the plant with the temporary StockID
                        PlantRepository.deletePlantByStockID(newPlant.StockID!!)

                        // Notify the UI with the new StockID
                        onStockIDFetched(response.StockID!!)

                        // Log the successful update
                        Log.d("SyncManager", "Successfully updated local database with new plant: $fetchedPlant")
                    }

                    // Step 5: Refresh the local plant list to ensure it's up to date with the main database
                    refreshLocalPlantList()
                }
            } catch (e: Exception) {
                Log.e("SyncManager", "Error during INSERT new plant sync: ${e.message}", e)
            }
        }
    }

    private suspend fun refreshLocalPlantList() {
        try {
            val mainDatabasePlants = ApiClient.apiService.getRRHE() // API call to get all plants from the main database
            PlantRepository.updateLocalPlants(mainDatabasePlants) // Update the local database with the latest records
            Log.d("SyncManager", "Local plant list refreshed with main database records.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Error refreshing local plant list: ${e.message}")
        }
    }

    private suspend fun fetchRRHEChangesWithRetry(retryCount: Int = 3, delayMillis: Long = 2000): List<Plant>? {
        var attempts = retryCount
        while (attempts > 0 && !shouldStopSyncing) {
            try {
                val changes = ApiClient.apiService.getRRHEChanges(PlantRepository.lastSyncTime)
                return changes.map { it.ensureNonNullValues() }
            } catch (e: HttpException) {
                Log.e("SyncManager", "HTTP error fetching changes: ${e.message}, retrying... attempts left: $attempts")
                delay(delayMillis)
                attempts--
            } catch (e: Exception) {
                Log.e("SyncManager", "Unexpected error fetching changes: ${e.message}, retrying... attempts left: $attempts")
                delay(delayMillis)
                attempts--
            }
        }
        return null
    }

    private suspend fun mergeChanges(remoteChanges: List<Plant>) {
        val localPlants = PlantRepository.plantDao.getAllPlants().associateBy { it.StockID }.toMutableMap()

        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (remotePlant in remoteChanges) {
            if (shouldStopSyncing) break

            val localPlant = localPlants[remotePlant.StockID]

            if (localPlant == null || isRemoteStampNewer(localPlant.Stamp, remotePlant.Stamp, dateTimeFormat)) {
                localPlants[remotePlant.StockID] = remotePlant.ensureNonNullValues()
            }
        }

        Log.d("SyncManager", "Merged ${remoteChanges.size} remote changes into local database at ${System.currentTimeMillis()}")
        PlantRepository.updateLocalPlants(localPlants.values.toList())
    }

    private fun isRemoteStampNewer(localStamp: String?, remoteStamp: String?, dateTimeFormat: SimpleDateFormat): Boolean {
        return try {
            val localDate = localStamp?.let { dateTimeFormat.parse(it) }
            val remoteDate = remoteStamp?.let { dateTimeFormat.parse(it) }
            remoteDate != null && (localDate == null || remoteDate.after(localDate))
        } catch (e: Exception) {
            Log.e("SyncManager", "Error parsing dates for comparison: ${e.message}", e)
            false
        }
    }

    suspend fun syncOnUserAction() {
        syncChangesWithRetry()
    }

    fun stopSyncing() {
        Log.d("SyncManager", "Stopping ongoing sync operations")
        shouldStopSyncing = true
        PlantRepository.isSyncing = false
    }

    suspend fun syncChangesWithRetry(retryCount: Int = 3, initialDelayMillis: Long = 2000) {
        var attempts = retryCount
        var currentDelay = initialDelayMillis
        while (attempts > 0 && !shouldStopSyncing) {
            try {
                val syncedSuccessfully = syncChanges()
                if (syncedSuccessfully && PlantRepository.isMainDatabaseConnected.value == true) {
                    return
                }
            } catch (e: Exception) {
                Log.e("SyncManager", "Error during sync changes, retrying... attempts left: $attempts")
                delay(currentDelay)
                currentDelay *= 2 // Exponential backoff
                attempts--
            }
        }
        if (attempts == 0) {
            Log.e("SyncManager", "Failed to connect to main database after $retryCount attempts.")
        }
    }
}
