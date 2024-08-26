package com.example.rrhe

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DataLoader {
    suspend fun loadPlantsFromDatabase() {
        withContext(Dispatchers.IO) {
            val plantList = PlantRepository.plantDao.getAllPlants()
            Log.d("DataLoader", "Loaded ${plantList.size} plants from database at ${System.currentTimeMillis()}")
            withContext(Dispatchers.Main) {
                PlantRepository.plantsMutable.postValue(plantList)
            }
        }
    }
}
