package com.example.rrhe

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats ORDER BY Stamp DESC")
    fun getAllStats(): List<Stats>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStats(stats: Stats)

    @Query("SELECT * FROM stats ORDER BY Stamp DESC LIMIT 1")
    fun getLatestStats(): Stats
}
