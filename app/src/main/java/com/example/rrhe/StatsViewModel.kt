package com.example.rrhe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel : ViewModel() {
    private val _stats = MutableStateFlow<Stats?>(null)
    val stats: StateFlow<Stats?> = _stats

    private var initialToastShown = false
    private var connectionToastShown = false

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize the StatsRepository and load initial data
                StatsRepository.initialize(MyApplication.instance.database.statsDao())

                // Fetch initial data from the local database
                StatsRepository.loadStatsFromDatabase()

                // Observe changes in the repository and update the state
                withContext(Dispatchers.Main) {
                    StatsRepository.stats.observeForever { newStats ->
                        _stats.value = newStats
                        if (!initialToastShown) {
                            showToast(MyApplication.instance, "Stats loaded locally")
                            initialToastShown = true
                        }
                    }
                }

                // Fetch and update stats from the remote server
                fetchAndUpdateStats()
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Error loading stats: ${e.message}")
            }
        }
    }

    private fun fetchAndUpdateStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                StatsRepository.fetchAndUpdateStats()
                withContext(Dispatchers.Main) {
                    if (StatsRepository.isMainDatabaseConnected && !connectionToastShown) {
                        showToast(MyApplication.instance, "Stats merged from main database")
                        connectionToastShown = true
                    }
                }
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Error fetching and updating stats: ${e.message}")
            }
        }
    }

    fun syncWithMainDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                StatsRepository.syncOnUserAction()
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Error syncing with main database: ${e.message}")
            }
        }
    }
}
