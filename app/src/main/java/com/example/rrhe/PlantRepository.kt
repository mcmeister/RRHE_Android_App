package com.example.rrhe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PlantRepository {
    private val _plants = MutableLiveData<List<Plant>>()
    val plants: LiveData<List<Plant>> get() = _plants

    private val _stats = MutableLiveData<Stats?>()
    val stats: LiveData<Stats?> get() = _stats

    private lateinit var plantDao: PlantDao
    private lateinit var statsDao: StatsDao
    private var lastSyncTime: String = "1970-01-01T00:00:00"
    private val unsyncedChanges = mutableListOf<PlantUpdateRequest>()

    private var isMainDatabaseConnected = false

    fun initialize(plantDao: PlantDao, statsDao: StatsDao) {
        this.plantDao = plantDao
        this.statsDao = statsDao
        loadPlantsFromDatabase()
        loadStatsFromDatabase()
        CoroutineScope(Dispatchers.IO).launch {
            initialDatabaseReplication()
        }
    }

    private fun loadPlantsFromDatabase() {
        runBlocking {
            val plantList = withContext(Dispatchers.IO) {
                plantDao.getAllPlants()
            }
            Log.d("PlantRepository", "Loaded ${plantList.size} plants from database")
            _plants.postValue(plantList)
        }
    }

    private fun loadStatsFromDatabase() {
        runBlocking {
            val statsData = withContext(Dispatchers.IO) {
                try {
                    statsDao.getAllStats().lastOrNull()
                } catch (e: Exception) {
                    Log.e("PlantRepository", "Error loading stats from database: ${e.message}")
                    null
                }
            }
            Log.d("PlantRepository", "Loaded stats from database: $statsData")
            _stats.postValue(statsData)
        }
    }

    private suspend fun initialDatabaseReplication() {
        try {
            val localPlants = plantDao.getAllPlants()
            val localStats = statsDao.getAllStats().lastOrNull()

            if (localPlants.isEmpty()) {
                val plants = ApiClient.apiService.getRRHE()
                plantDao.insertPlants(plants.map { it.ensureNonNullValues() })
                Log.d("PlantRepository", "Replicated ${plants.size} plants from API to database")
                _plants.postValue(plantDao.getAllPlants())
            }

            if (localStats == null) {
                val statsData = ApiClient.apiService.getStats()
                statsDao.insertStats(statsData)
                Log.d("PlantRepository", "Replicated stats from API to database")
                _stats.postValue(statsDao.getAllStats().lastOrNull())
            }

            lastSyncTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            isMainDatabaseConnected = true
        } catch (e: Exception) {
            Log.e("PlantRepository", "Exception during initial replication: ${e.message}")
            isMainDatabaseConnected = false
        }
    }

    suspend fun updatePlants(plants: List<Plant>) {
        withContext(Dispatchers.IO) {
            Log.d("PlantRepository", "Updating ${plants.size} plants")
            plantDao.insertPlants(plants.map { it.ensureNonNullValues() })
            _plants.postValue(plantDao.getAllPlants())
        }
    }

    private suspend fun updatePlant(plant: Plant) {
        withContext(Dispatchers.IO) {
            Log.d("PlantRepository", "Updating plant with ID: ${plant.StockID}")
            plantDao.updatePlant(plant.ensureNonNullValues())
            _plants.postValue(plantDao.getAllPlants())
        }
    }

    suspend fun savePlantUpdate(plant: Plant) {
        val updatedPlant = plant.copy(Stamp = Date())
        updatePlant(updatedPlant)
        val updateRequest = PlantUpdateRequest.fromPlant(updatedPlant)
        unsyncedChanges.add(updateRequest)
        syncChanges()
    }

    suspend fun syncChanges() {
        withContext(Dispatchers.IO) {
            try {
                if (isMainDatabaseConnected) {
                    val iterator = unsyncedChanges.iterator()
                    while (iterator.hasNext()) {
                        val updateRequest = iterator.next()
                        val updateResponse = try {
                            ApiClient.apiService.updatePlant(updateRequest)
                        } catch (e: HttpException) {
                            null
                        }

                        if (updateResponse != null) {
                            iterator.remove()
                        } else {
                            Log.e("PlantRepository", "Sync changes failed")
                        }
                    }

                    val changes = try {
                        ApiClient.apiService.getRRHEChanges(lastSyncTime)
                    } catch (e: HttpException) {
                        Log.e("PlantRepository", "Fetching changes failed with exception: ${e.message}")
                        null
                    }

                    changes?.let {
                        if (it.isNotEmpty()) {
                            mergeChanges(it)
                            _plants.postValue(plantDao.getAllPlants())
                            lastSyncTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                        }
                    }

                    fetchAndUpdateStats()
                } else {
                    Log.e("PlantRepository", "No connection to main database")
                }
            } catch (e: Exception) {
                Log.e("PlantRepository", "An unexpected error occurred during sync: ${e.message}")
            }
        }
    }

    private suspend fun fetchAndUpdateStats() {
        withContext(Dispatchers.IO) {
            try {
                if (isMainDatabaseConnected) {
                    val statsData = ApiClient.apiService.getStats()
                    statsDao.insertStats(statsData)
                    _stats.postValue(statsDao.getAllStats().lastOrNull())
                } else {
                    Log.e("PlantRepository", "No connection to main database, fetching local stats")
                    val localStats = statsDao.getAllStats().lastOrNull()
                    _stats.postValue(localStats)
                }
            } catch (e: HttpException) {
                Log.e("PlantRepository", "Fetching stats failed with exception: ${e.message}")
            } catch (e: Exception) {
                Log.e("PlantRepository", "An unexpected error occurred while fetching stats: ${e.message}")
            }
        }
    }

    private suspend fun mergeChanges(remoteChanges: List<Plant>) {
        withContext(Dispatchers.IO) {
            val localPlants = plantDao.getAllPlants().associateBy { it.StockID }.toMutableMap()

            for (remotePlant in remoteChanges) {
                val localPlant = localPlants[remotePlant.StockID]

                if (localPlant == null || localPlant.Stamp.before(remotePlant.Stamp)) {
                    localPlants[remotePlant.StockID] = remotePlant
                }
            }

            Log.d("PlantRepository", "Merged ${remoteChanges.size} remote changes into local database")
            plantDao.insertPlants(localPlants.values.toList())
        }
    }
}
