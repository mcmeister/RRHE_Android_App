package com.example.rrhe

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Plant::class, Stats::class, NameEntity::class], version = 14, exportSchema = false)
@TypeConverters(DateTypeConverter::class, PairTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun statsDao(): StatsDao
    abstract fun namesDao(): NamesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    /* .setQueryCallback({ sqlQuery: String, bindArgs: List<Any?> ->
                        Log.d("RoomQuery", "Query: $sqlQuery, Args: $bindArgs")
                    }, Executors.newSingleThreadExecutor()) */
                    .addMigrations(MIGRATION_13_14)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
