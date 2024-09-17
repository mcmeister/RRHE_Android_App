package com.example.rrhe

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val _plants = MutableStateFlow<List<Plant>>(emptyList())
    val plants: StateFlow<List<Plant>> = _plants

    private var searchQuery = MutableStateFlow("")
    private var filterMother = MutableStateFlow(false)
    private var filterWebsite = MutableStateFlow(false)
    private var initialToastShown = false
    private var connectionToastShown = false

    init {
        loadPlants()

        // Trigger sync with the main database right after loading local plants
        syncWithMainDatabase()
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
                            .filter { it.StockID!! > 0 }  // Filter out negative StockIDs
                            .sortedByDescending { it.StockID }  // Sort by StockID descending
                        performSearch(searchQuery.value, filterMother.value, filterWebsite.value)
                        if (!initialToastShown) {
                            showToast(
                                getApplication<Application>().applicationContext,
                                "Plant list loaded locally"
                            )
                            initialToastShown = true
                        }
                    }
                }

                // Show "Working with local database" message only once
                withContext(Dispatchers.Main) {
                    if (!initialToastShown) {
                        showToast(getApplication<Application>().applicationContext, "Working with local database")
                        initialToastShown = true
                    }
                }

                // Check and fetch initial data from the remote server
                PlantRepository.checkAndFetchInitialData(getApplication<Application>().applicationContext)

                // Wait until the main database connection is established
                withContext(Dispatchers.Main) {
                    PlantRepository.isMainDatabaseConnected.observeForever { isConnected ->
                        if (isConnected && !connectionToastShown) {
                            showToast(getApplication<Application>().applicationContext, "Connected to main database")
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
            PlantRepository.plants.value?.filter { it.StockID!! > 0 }?.sortedByDescending { it.StockID } ?: emptyList()
        } else {
            PlantRepository.plants.value?.filter {
                val matchesQuery = query.isEmpty() ||
                        it.StockID.toString() == query ||
                        it.Family?.contains(query, ignoreCase = true) == true ||
                        it.Species?.contains(query, ignoreCase = true) == true ||
                        it.Subspecies?.contains(query, ignoreCase = true) == true ||
                        it.StockQty.toString().contains(query) ||
                        it.StockPrice.toString().contains(query) ||
                        it.AddedBy?.contains(query, ignoreCase = true) == true ||
                        it.PlantedEnd == query

                val matchesMother = !filterMother || it.Mother == 1
                val matchesWebsite = !filterWebsite || it.Website == 1
                matchesQuery && matchesMother && matchesWebsite
            }?.filter { it.StockID!! > 0 }?.sortedByDescending { it.StockID } ?: emptyList()
        }
    }

    fun updatePlantList(plants: List<Plant>) {
        _plants.value = plants.filter { it.StockID!! > 0 }.sortedByDescending { it.StockID }
    }

    private fun syncWithMainDatabase() {
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
                val refreshedPlants = PlantRepository.plantDao.getAllPlants()
                    .filter { it.StockID!! > 0 }
                    .sortedByDescending { it.StockID }
                withContext(Dispatchers.Main) {
                    _plants.value = refreshedPlants
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error refreshing plant list: ${e.message}")
            }
        }
    }
}
