package com.example.rrhe

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the Stamp column as INTEGER NOT NULL with a default value
        database.execSQL("ALTER TABLE plants ADD COLUMN Stamp INTEGER NOT NULL DEFAULT 0")
    }
}
