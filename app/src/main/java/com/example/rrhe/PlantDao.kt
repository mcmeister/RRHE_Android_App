package com.example.rrhe

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    // Retrieve all plants from the database
    @Query("SELECT * FROM plants")
    fun getAllPlants(): List<Plant>

    // Insert a list of plants into the database, replacing any existing entries with the same primary key
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlants(plants: List<Plant>)

    // Update a single plant's data in the database
    @Update
    fun updatePlant(plant: Plant)

    // Retrieve a specific plant by its StockID, returning a Flow for reactive updates
    @Query("SELECT * FROM plants WHERE StockID = :stockID")
    fun getPlantByStockID(stockID: Int): Flow<Plant?>

    // Update stock values (TotalValue, USD, EUR) for a specific plant identified by StockID
    @Query("UPDATE plants SET TotalValue = :totalValue, USD = :usdValue, EUR = :eurValue WHERE StockID = :stockID")
    fun updateStockValues(stockID: Int, totalValue: Double, usdValue: Double, eurValue: Double)

    // Retrieve a list of distinct Family names from the database
    @Query("SELECT DISTINCT Family FROM plants")
    fun listFamily(): List<String>

    // Retrieve a list of distinct Species names filtered by a specific Family
    @Query("SELECT DISTINCT Species FROM plants WHERE Family = :familyName")
    fun listSpeciesByFamily(familyName: String): List<String>

    // Retrieve a list of distinct Subspecies names filtered by a specific Family and Species
    @Query("SELECT DISTINCT Subspecies FROM plants WHERE Family = :familyName AND Species = :speciesName")
    fun listSubspeciesByFamilyAndSpecies(familyName: String, speciesName: String): List<String>

    // Retrieve plants that belong to a specific Family and have Mother = 1
    @Query("SELECT * FROM plants WHERE Family = :family AND Mother = :mother")
    fun getPlantsByFamilyAndMother(family: String, mother: Int): List<Plant>

    // Retrieve plants where Mother = 0, typically for Father plants
    @Query("SELECT * FROM plants WHERE Mother = :mother")
    fun getPlantsWithMother(mother: Int): List<Plant>

    // Delete plants with negative StockIDs
    @Query("DELETE FROM plants WHERE StockID = :stockID AND StockID < 0")
    fun deleteByStockID(stockID: Int)

    @Query("SELECT * FROM plants WHERE PlantedEnd = :todayDate")
    fun getPlantsReadyForNotification(todayDate: String?): List<Plant?>?
}
