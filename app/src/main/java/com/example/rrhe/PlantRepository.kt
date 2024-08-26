package com.example.rrhe

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.rrhe.AppDatabase.Companion.getDatabase
import com.example.rrhe.DataLoader.loadPlantsFromDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object PlantRepository {
    val plantsMutable = MutableLiveData<List<Plant>>()
    val plants: LiveData<List<Plant>> get() = plantsMutable

    lateinit var plantDao: PlantDao
    private lateinit var statsDao: StatsDao

    var lastSyncTime: String = "1970-01-01 00:00:00"
    val unsyncedChanges = mutableListOf<PlantUpdateRequest>()

    private val _isMainDatabaseConnected = MutableLiveData(false)
    val isMainDatabaseConnected: LiveData<Boolean> get() = _isMainDatabaseConnected

    var isSyncing = false
    private var connectionCheckInitiated = false

    private var cachedFamilies: List<String>? = null

    fun initialize(plantDao: PlantDao, statsDao: StatsDao) {
        this.plantDao = plantDao
        this.statsDao = statsDao
    }

    suspend fun checkAndFetchInitialData(context: Context) {
        if (connectionCheckInitiated) {
            Log.d("PlantRepository", "Connection check already initiated, skipping redundant checks.")
            return
        }

        connectionCheckInitiated = true

        // Load plants from local database first
        Log.d("PlantRepository", "Loading plants from local database")
        loadPlantsFromDatabase()
        Log.d("PlantRepository", "Plants loaded from local database")

        withContext(Dispatchers.IO) {
            Log.d("PlantRepository", "Checking main database connection")
            DatabaseConnectionManager.checkMainDatabaseConnectionWithRetry()
            Log.d("PlantRepository", "Main database connection check completed")

            if (_isMainDatabaseConnected.value == true) {
                if (plantsMutable.value.isNullOrEmpty()) {
                    Log.d("PlantRepository", "Local database is empty, replicating main database")
                    ReplicationManager.initialDatabaseReplicationWithRetry()
                    Log.d("PlantRepository", "Main database replication completed")
                } else {
                    Log.d("PlantRepository", "Local database is not empty, skipping replication")
                }
            }

            if (_isMainDatabaseConnected.value == true && unsyncedChanges.isNotEmpty()) {
                Log.d("PlantRepository", "Starting background sync process")
                SyncManager.syncChangesWithRetry()
                Log.d("PlantRepository", "Background sync process started")
            } else {
                Log.d("PlantRepository", "Skipping background sync: No connection or no unsynced changes")
            }

            Log.d("PlantRepository", "Populating names table from all plants")
            val allPlants = plantsMutable.value ?: emptyList()
            populateNamesTable(context, allPlants)
            Log.d("PlantRepository", "Names table populated")
        }
    }

    suspend fun getPlantByStockID(stockID: Int): Plant? {
        return withContext(Dispatchers.IO) {
            plantDao.getPlantByStockID(stockID).firstOrNull()
        }
    }

    suspend fun deletePlantByStockID(stockID: Int) {
        withContext(Dispatchers.IO) {
            plantDao.deleteByStockID(stockID)
            Log.d("PlantRepository", "Deleted plant with StockID: $stockID")
        }

        val updatedPlants = plantDao.getAllPlants()
        withContext(Dispatchers.Main) {
            plantsMutable.postValue(updatedPlants)
        }
    }

    suspend fun updateStockRow(stockID: Int, stockPrice: Double, stockQty: Int) {
        val (latestUSD, latestEUR) = getLatestExchangeRatesFromLocal()
        val totalValue = stockPrice * stockQty
        val usdValue = stockPrice * latestUSD
        val eurValue = stockPrice * latestEUR

        withContext(Dispatchers.IO) {
            plantDao.updateStockValues(stockID, totalValue, usdValue, eurValue)
        }
    }

    suspend fun getLatestExchangeRatesFromLocal(): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            val latestStats = statsDao.getLatestStats()
            Pair(latestStats.usd, latestStats.eur)
        }
    }

    suspend fun updateLocalPlants(plants: List<Plant>) {
        withContext(Dispatchers.IO) {
            plantDao.insertPlants(plants)
            val allPlants = plantDao.getAllPlants()
            withContext(Dispatchers.Main) {
                plantsMutable.postValue(allPlants)
            }
        }
    }

    suspend fun updatePlant(plant: Plant) {
        withContext(Dispatchers.IO) {
            plantDao.updatePlant(plant)
        }
        val updatedPlants = plantDao.getAllPlants()
        withContext(Dispatchers.Main) {
            plantsMutable.postValue(updatedPlants)
        }
    }

    fun setMainDatabaseConnected(isConnected: Boolean) {
        _isMainDatabaseConnected.postValue(isConnected)
    }

    suspend fun populateNamesTable(context: Context, plants: List<Plant>) {
        Log.d("PlantRepository", "Starting to populate names table")
        withContext(Dispatchers.IO) {
            val database = getDatabase(context)

            Log.d("PlantRepository", "Processing ${plants.size} plants to populate names table")

            plants.forEach { plant ->
                val nameEntity = NameEntity(0, plant.Family ?: "", plant.Species ?: "", plant.Subspecies ?: "")

                val exists = database.namesDao().getNameId(plant.Family ?: "", plant.Species ?: "", plant.Subspecies ?: "") != null
                if (!exists) {
                    database.namesDao().insertName(nameEntity)
                }
            }
        }
        Log.d("PlantRepository", "Finished populating names table")
    }

    suspend fun getUniqueFamilies(context: Context): List<String> = withContext(Dispatchers.IO) {
        Log.d("PlantRepository", "Attempting to fetch unique families from the database")

        if (cachedFamilies == null) {
            try {
                val database = getDatabase(context)
                cachedFamilies = database.namesDao().getUniqueFamilies().also { families ->
                    Log.d("PlantRepository", "Fetched ${families.size} unique families from the database")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("PlantRepository", "Error fetching unique families: ${e.message}")
                cachedFamilies = emptyList()
            }
        } else {
            Log.d("PlantRepository", "Using cached unique families")
        }

        return@withContext cachedFamilies ?: emptyList()
    }

    suspend fun getSpeciesByFamily(context: Context, family: String): List<String> = withContext(Dispatchers.IO) {
        Log.d("PlantRepository", "Attempting to fetch species for family '$family' from the database")
        try {
            val database = getDatabase(context)
            val species = database.namesDao().getSpeciesByFamily(family)
            Log.d("PlantRepository", "Fetched ${species.size} species for family '$family' from the database")
            species
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PlantRepository", "Error fetching species: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSubspeciesByFamilyAndSpecies(context: Context, family: String, species: String): List<String> = withContext(Dispatchers.IO) {
        Log.d("PlantRepository", "Attempting to fetch subspecies for family '$family' and species '$species' from the database")
        try {
            val database = getDatabase(context)
            val subspecies = database.namesDao().getSubspeciesByFamilyAndSpecies(family, species)
            Log.d("PlantRepository", "Fetched ${subspecies.size} subspecies for family '$family' and species '$species' from the database")
            subspecies
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PlantRepository", "Error fetching subspecies: ${e.message}")
            emptyList()
        }
    }

    fun isFamilyExists(context: Context, familyName: String): Boolean {
        val database = getDatabase(context)
        return database.namesDao().getFamily(familyName) != null
    }

    fun isSpeciesExists(context: Context, familyName: String, speciesName: String): Boolean {
        val database = getDatabase(context)
        return database.namesDao().getSpecies(familyName, speciesName) != null
    }

    private fun isSubspeciesExists(context: Context, familyName: String, speciesName: String, subspeciesName: String): Boolean {
        val database = getDatabase(context)
        return database.namesDao().getSubspecies(familyName, speciesName, subspeciesName) != null
    }

    suspend fun updateUniqueEntries(context: Context, familyName: String, speciesName: String, subspeciesName: String) {
        withContext(Dispatchers.IO) {
            if (!isFamilyExists(context, familyName)) {
                addNewNameEntry(context, familyName, speciesName, subspeciesName)
            }

            if (!isSpeciesExists(context, familyName, speciesName)) {
                addNewNameEntry(context, familyName, speciesName, subspeciesName)
            }

            if (!isSubspeciesExists(context, familyName, speciesName, subspeciesName)) {
                addNewNameEntry(context, familyName, speciesName, subspeciesName)
            }
        }
    }

    private fun addNewNameEntry(context: Context, familyName: String, speciesName: String, subspeciesName: String) {
        val database = getDatabase(context)
        val nameEntity = NameEntity(0, familyName, speciesName, subspeciesName)
        database.namesDao().insertName(nameEntity)
    }

    suspend fun getMotherPlantsByFamily(family: String): List<Plant> {
        return withContext(Dispatchers.IO) {
            val motherPlants = plantDao.getPlantsByFamilyAndMother(family, 1)
            Log.d("PlantRepository", "Mother plants fetched: ${motherPlants.size}")
            motherPlants
        }
    }

    suspend fun getFatherPlants(): List<Plant> {
        return withContext(Dispatchers.IO) {
            val fatherPlants = plantDao.getPlantsWithMother(0)
            Log.d("PlantRepository", "Father plants fetched: ${fatherPlants.size}")
            fatherPlants
        }
    }

    suspend fun saveNewPlantLocally(context: Context, plant: Plant) {
        withContext(Dispatchers.IO) {
            getDatabase(context)
            plantDao.insertPlants(listOf(plant))  // Insert the new plant with temporary StockID
        }
        // Optionally, update the LiveData
        val updatedPlants = plantDao.getAllPlants()
        withContext(Dispatchers.Main) {
            plantsMutable.postValue(updatedPlants)
        }
    }
}
