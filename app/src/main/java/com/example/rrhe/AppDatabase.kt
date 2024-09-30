package com.example.rrhe

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Plant::class, Stats::class, NameEntity::class, Website::class], version = 15, exportSchema = false)
@TypeConverters(DateTypeConverter::class, PairTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun statsDao(): StatsDao
    abstract fun namesDao(): NamesDao
    abstract fun websiteDao(): WebsiteDao

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
                    .addMigrations(MIGRATION_13_14, MIGRATION_14_15)  // Update the migration to version 15
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
