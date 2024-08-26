package com.example.rrhe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rrhe.PlantRepository.plantDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockViewModel : ViewModel() {
    private val _plants = MutableStateFlow<List<Plant>>(emptyList())
    val plants: StateFlow<List<Plant>> = _plants

    private var searchQuery = MutableStateFlow("")
    private var filterMother = MutableStateFlow(false)
    private var filterWebsite = MutableStateFlow(false)
    private var initialToastShown = false
    private var connectionToastShown = false

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize the repository and load initial data
                PlantRepository.initialize(
                    MyApplication.instance.database.plantDao(),
                    MyApplication.instance.database.statsDao()
                )

                // Load initial plants from local database
                DataLoader.loadPlantsFromDatabase()

                // Observe changes in the repository and update the state
                withContext(Dispatchers.Main) {
                    PlantRepository.plants.observeForever { newPlants ->
                        _plants.value = newPlants
                        performSearch(searchQuery.value, filterMother.value, filterWebsite.value)
                        if (!initialToastShown) {
                            showToast(
                                MyApplication.instance,
                                "Plant list loaded locally"
                            )
                            initialToastShown = true
                        }
                    }
                }

                // Show "Working with local database" message only once
                withContext(Dispatchers.Main) {
                    if (!initialToastShown) {
                        showToast(MyApplication.instance, "Working with local database")
                        initialToastShown = true
                    }
                }

                // Check and fetch initial data from the remote server
                PlantRepository.checkAndFetchInitialData(MyApplication.instance.applicationContext)

                // Wait until the main database connection is established
                withContext(Dispatchers.Main) {
                    PlantRepository.isMainDatabaseConnected.observeForever { isConnected ->
                        if (isConnected && !connectionToastShown) {
                            showToast(MyApplication.instance, "Connected to main database")
                            connectionToastShown = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error loading plants: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String, filterMother: Boolean, filterWebsite: Boolean) {
        searchQuery.value = query
        this.filterMother.value = filterMother
        this.filterWebsite.value = filterWebsite
        performSearch(query, filterMother, filterWebsite)
    }

    private fun performSearch(query: String, filterMother: Boolean, filterWebsite: Boolean) {
        _plants.value = if (query.isEmpty() && !filterMother && !filterWebsite) {
            PlantRepository.plants.value ?: emptyList()
        } else {
            val lowercaseQuery = query.lowercase()
            PlantRepository.plants.value?.filter {
                val matchesQuery = query.isEmpty() ||
                        it.StockID.toString() == query ||
                        it.Family?.contains(lowercaseQuery, ignoreCase = true) == true ||
                        it.Species?.contains(lowercaseQuery, ignoreCase = true) == true ||
                        it.Subspecies?.contains(lowercaseQuery, ignoreCase = true) == true ||
                        it.StockQty.toString().contains(lowercaseQuery) ||
                        it.StockPrice.toString().contains(lowercaseQuery) ||
                        it.AddedBy?.contains(lowercaseQuery, ignoreCase = true) == true

                val matchesMother = !filterMother || it.Mother == 1
                val matchesWebsite = !filterWebsite || it.Website == 1
                matchesQuery && matchesMother && matchesWebsite
            } ?: emptyList()
        }
    }

    fun updatePlantList(plants: List<Plant>) {
        _plants.value = plants
    }

    fun syncWithMainDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SyncManager.syncOnUserAction()
                // Refresh the plant list after syncing
                refreshPlantList()
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error syncing with main database: ${e.message}")
            }
        }
    }

    fun checkDatabaseConnectionOnLaunch() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DatabaseConnectionManager.checkMainDatabaseConnectionWithRetry()
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error checking database connection on launch: ${e.message}")
            }
        }
    }

    private suspend fun refreshPlantList() {
        withContext(Dispatchers.IO) {
            try {
                val refreshedPlants = plantDao.getAllPlants()
                withContext(Dispatchers.Main) {
                    _plants.value = refreshedPlants
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error refreshing plant list: ${e.message}")
            }
        }
    }
}
