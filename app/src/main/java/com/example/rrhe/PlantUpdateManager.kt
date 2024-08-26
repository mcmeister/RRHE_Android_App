package com.example.rrhe

import android.content.Context
import android.util.Log
import com.example.rrhe.PlantRepository.plantsMutable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object PlantUpdateManager {
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun savePlantUpdate(context: Context, plant: Plant) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val updatedPlant = plant.copy(
            Stamp = dateTimeFormat.format(Date()), // Use dateTimeFormat for Stamp
            PlantedStart = plant.PlantedStart?.let { startDate ->
                dateFormat.parse(startDate)?.let { parsedDate ->
                    dateFormat.format(parsedDate)
                }
            },
            PlantedEnd = plant.PlantedEnd?.let { endDate ->
                dateFormat.parse(endDate)?.let { parsedDate ->
                    dateFormat.format(parsedDate)
                }
            },
            PollinateDate = plant.PollinateDate?.let { pollinateDate ->
                dateFormat.parse(pollinateDate)?.let { parsedDate ->
                    dateFormat.format(parsedDate)
                }
            },
            SeedsPlanted = plant.SeedsPlanted?.let { seedsPlantedDate ->
                dateFormat.parse(seedsPlantedDate)?.let { parsedDate ->
                    dateFormat.format(parsedDate)
                }
            },
            SeedsHarvest = plant.SeedsHarvest?.let { seedsHarvestDate ->
                dateFormat.parse(seedsHarvestDate)?.let { parsedDate ->
                    dateFormat.format(parsedDate)
                }
            }
        )

        updatePlantLocally(updatedPlant)

        val updateRequest = PlantUpdateRequest.fromPlant(updatedPlant)
        PlantRepository.unsyncedChanges.add(updateRequest)

        GlobalScope.launch {
            plant.StockID?.let { stockID ->
                plant.StockPrice?.let { stockPrice ->
                    PlantRepository.updateStockRow(stockID, stockPrice.toDouble(), plant.StockQty)
                }
            }
            val allPlants = plantsMutable.value ?: emptyList()
            PlantRepository.populateNamesTable(context, allPlants)
            SyncManager.syncChangesWithRetry()
        }
    }

    private suspend fun updatePlantLocally(plant: Plant) {
        withContext(Dispatchers.IO) {
            Log.d("PlantUpdateManager", "Updating plant with ID: ${plant.StockID} at ${System.currentTimeMillis()}")
            PlantRepository.plantDao.updatePlant(plant.ensureNonNullValues())
            val allPlants = PlantRepository.plantDao.getAllPlants()
            withContext(Dispatchers.Main) {
                plantsMutable.postValue(allPlants)
            }
        }
    }
}
