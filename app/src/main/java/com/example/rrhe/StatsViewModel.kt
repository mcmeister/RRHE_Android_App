package com.example.rrhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class StatsViewModel : ViewModel() {
    private val _stats = MutableStateFlow<Stats?>(null)
    val stats: StateFlow<Stats?> = _stats

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val app = MyApplication.instance
            val statsDao = app.database.statsDao()
            val statsData = withContext(Dispatchers.IO) {
                try {
                    val allStats = statsDao.getAllStats()
                    if (allStats.isNotEmpty()) allStats.last() else null
                } catch (e: Exception) {
                    Log.e("StatsViewModel", "Error fetching stats: ${e.message}")
                    null
                }
            }
            if (statsData != null) {
                Log.d("StatsViewModel", "Stats fetched: $statsData")
            } else {
                Log.d("StatsViewModel", "Stats is null")
            }
            _stats.value = statsData
        }
    }
}
