package com.example.rrhe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class StockViewModel : ViewModel() {
    private val _plants = MutableStateFlow<List<Plant>>(emptyList())
    val plants: StateFlow<List<Plant>> = _plants

    private var searchQuery = MutableStateFlow("")

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                PlantRepository.initialize(MyApplication.instance.database.plantDao(), MyApplication.instance.database.statsDao())
                PlantRepository.syncChanges()
                fetchRRHEWithRetry()
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error initializing PlantRepository: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        performSearch(query)
    }

    private fun fetchRRHEWithRetry(retryCount: Int = 3) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getRRHE()
                _plants.value = response.map { it.ensureNonNullValues() }
                Log.d("StockViewModel", "Fetched ${_plants.value.size} plants from API")
                PlantRepository.updatePlants(_plants.value)
            } catch (e: HttpException) {
                Log.e("StockViewModel", "HTTP error fetching plants: ${e.message}, response: ${e.response()?.errorBody()?.string()}")
                if (retryCount > 0) {
                    Log.e("StockViewModel", "Retrying... attempts left: $retryCount")
                    fetchRRHEWithRetry(retryCount - 1)
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Unexpected error fetching plants: ${e.message}")
            }
        }
    }

    private fun performSearch(query: String) {
        _plants.value = if (query.isEmpty()) {
            PlantRepository.plants.value ?: emptyList()
        } else {
            val lowercaseQuery = query.lowercase()
            PlantRepository.plants.value?.filter {
                it.NameConcat?.contains(lowercaseQuery, ignoreCase = true) == true ||
                        it.StockID.toString() == query ||
                        it.StockPrice.toString().contains(lowercaseQuery)
            } ?: emptyList()
        }
    }

    fun updatePlantList(plants: List<Plant>) {
        _plants.value = plants
    }
}
