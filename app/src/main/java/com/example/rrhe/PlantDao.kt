package com.example.rrhe

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants")
    fun getAllPlants(): List<Plant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlants(plants: List<Plant>)

    @Update
    fun updatePlant(plant: Plant)
}