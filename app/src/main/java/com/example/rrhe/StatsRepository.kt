package com.example.rrhe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import retrofit2.HttpException

object StatsRepository {
    private val _stats = MutableLiveData<Stats?>()
    val stats: LiveData<Stats?> get() = _stats

    private lateinit var statsDao: StatsDao

    var isMainDatabaseConnected = false
    private var isSyncing = false

    fun initialize(statsDao: StatsDao) {
        this.statsDao = statsDao
    }

    suspend fun loadStatsFromDatabase() {
        withContext(Dispatchers.IO) {
            val statsData = try {
                statsDao.getAllStats()
            } catch (e: Exception) {
                Log.e("StatsRepository", "Error loading stats from database: ${e.message}")
                emptyList()
            }
            Log.d("StatsRepository", "Loaded ${statsData.size} stats from database at ${System.currentTimeMillis()}")
            withContext(Dispatchers.Main) {
                if (statsData.isNotEmpty()) {
                    _stats.postValue(statsData.maxByOrNull { it.stamp }) // Display the latest row
                }
            }
        }
    }

    suspend fun fetchAndUpdateStats() {
        fetchAndUpdateStatsWithRetry()
    }

    private suspend fun fetchAndUpdateStatsWithRetry(retryCount: Int = 3, delayMillis: Long = 2000) {
        var attempts = retryCount
        while (attempts > 0) {
            try {
                withContext(Dispatchers.IO) {
                    val statsList = fetchStatsWithRetry()
                    isMainDatabaseConnected = true
                    statsList.forEach { statsDao.insertStats(it) }
                    val allStats = statsDao.getAllStats()
                    Log.d("StatsRepository", "Fetched and updated ${allStats.size} stats from API at ${System.currentTimeMillis()}")
                    withContext(Dispatchers.Main) {
                        if (allStats.isNotEmpty()) {
                            _stats.postValue(allStats.maxByOrNull { it.stamp }) // Display the latest row
                        }
                    }
                }
                return
            } catch (e: HttpException) {
                isMainDatabaseConnected = false
                Log.e("StatsRepository", "Fetching stats failed with exception: ${e.message}, retrying... attempts left: $attempts")
                fetchLocalStats()
            } catch (e: Exception) {
                isMainDatabaseConnected = false
                Log.e("StatsRepository", "An unexpected error occurred while fetching stats: ${e.message}, retrying... attempts left: $attempts")
                fetchLocalStats()
            }
            delay(delayMillis)
            attempts--
        }
    }

    private suspend fun fetchLocalStats() {
        val localStats = statsDao.getAllStats()
        withContext(Dispatchers.Main) {
            if (localStats.isNotEmpty()) {
                _stats.postValue(localStats.maxByOrNull { it.stamp }) // Display the latest row
            }
        }
    }

    suspend fun syncOnUserAction() {
        syncOnUserActionWithRetry()
    }

    private suspend fun syncOnUserActionWithRetry(retryCount: Int = 3, delayMillis: Long = 2000) {
        var attempts = retryCount
        while (attempts > 0) {
            if (isSyncing) return
            isSyncing = true

            try {
                withContext(Dispatchers.IO) {
                    val statsList = fetchStatsWithRetry()
                    isMainDatabaseConnected = true
                    statsList.forEach { statsDao.insertStats(it) }
                    val allStats = statsDao.getAllStats()
                    Log.d("StatsRepository", "Fetched and updated ${allStats.size} stats from API at ${System.currentTimeMillis()}")
                    withContext(Dispatchers.Main) {
                        if (allStats.isNotEmpty()) {
                            _stats.postValue(allStats.maxByOrNull { it.stamp }) // Display the latest row
                        }
                    }
                }
                return
            } catch (e: HttpException) {
                isMainDatabaseConnected = false
                Log.e("StatsRepository", "Fetching stats failed with exception: ${e.message}, retrying... attempts left: $attempts")
                fetchLocalStats()
            } catch (e: Exception) {
                isMainDatabaseConnected = false
                Log.e("StatsRepository", "An unexpected error occurred while fetching stats: ${e.message}, retrying... attempts left: $attempts")
                fetchLocalStats()
            } finally {
                isSyncing = false
            }
            delay(delayMillis)
            attempts--
        }
    }

    private suspend fun fetchStatsWithRetry(retryCount: Int = 3): List<Stats> {
        var attempts = retryCount
        while (attempts > 0) {
            try {
                return ApiClient.apiService.getStats()
            } catch (e: HttpException) {
                Log.e("StatsRepository", "HTTP error fetching stats: ${e.message}, retrying... attempts left: $attempts")
                if (--attempts == 0) throw e
            } catch (e: Exception) {
                Log.e("StatsRepository", "Unexpected error fetching stats: ${e.message}, retrying... attempts left: $attempts")
                if (--attempts == 0) throw e
            }
        }
        throw Exception("Failed to fetch stats after $retryCount attempts")
    }
}
