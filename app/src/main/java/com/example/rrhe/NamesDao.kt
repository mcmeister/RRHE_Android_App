package com.example.rrhe

import androidx.room.*

@Dao
interface NamesDao {
    @Query("SELECT DISTINCT family FROM names")
    fun getUniqueFamilies(): List<String>

    @Query("SELECT DISTINCT species FROM names WHERE family = :family")
    fun getSpeciesByFamily(family: String): List<String>

    @Query("SELECT DISTINCT subspecies FROM names WHERE family = :family AND species = :species")
    fun getSubspeciesByFamilyAndSpecies(family: String, species: String): List<String>

    @Query("SELECT nameID FROM names WHERE family = :family AND species = :species AND subspecies = :subspecies LIMIT 1")
    fun getNameId(family: String, species: String, subspecies: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertName(name: NameEntity)

    @Query("SELECT family FROM names WHERE family = :family LIMIT 1")
    fun getFamily(family: String): String?

    @Query("SELECT species FROM names WHERE family = :family AND species = :species LIMIT 1")
    fun getSpecies(family: String, species: String): String?

    @Query("SELECT subspecies FROM names WHERE family = :family AND species = :species AND subspecies = :subspecies LIMIT 1")
    fun getSubspecies(family: String, species: String, subspecies: String): String?
}

@Entity(tableName = "names")
data class NameEntity(
    @PrimaryKey(autoGenerate = true) val nameID: Int,
    val family: String,
    val species: String,
    val subspecies: String
)
