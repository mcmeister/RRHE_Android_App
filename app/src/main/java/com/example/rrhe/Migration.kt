package com.example.rrhe

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

fun doesColumnExist(database: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
    val cursor = database.query("PRAGMA table_info($tableName)")
    cursor.use {
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == columnName) {
                return true
            }
        }
    }
    return false
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `names` (
                `name_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `familyName` TEXT NOT NULL,
                `speciesName` TEXT,
                `subspeciesName` TEXT
            )
        """)

        if (!doesColumnExist(db, "plants", "StockID")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN StockID INTEGER PRIMARY KEY AUTOINCREMENT")
        }
        if (!doesColumnExist(db, "plants", "M_ID")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN M_ID INTEGER")
        }
        if (!doesColumnExist(db, "plants", "F_ID")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN F_ID INTEGER")
        }
        if (!doesColumnExist(db, "plants", "NameConcat")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN NameConcat TEXT")
        }
        if (!doesColumnExist(db, "plants", "Family")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Family TEXT")
        }
        if (!doesColumnExist(db, "plants", "Species")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Species TEXT")
        }
        if (!doesColumnExist(db, "plants", "SubSpecies")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN SubSpecies TEXT")
        }
        if (!doesColumnExist(db, "plants", "ThaiName")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN ThaiName TEXT")
        }
        if (!doesColumnExist(db, "plants", "PlantDescription")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PlantDescription TEXT")
        }
        if (!doesColumnExist(db, "plants", "TableName")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN TableName TEXT")
        }
        if (!doesColumnExist(db, "plants", "Mother")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Mother INTEGER")
        }
        if (!doesColumnExist(db, "plants", "Website")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Website INTEGER")
        }
        if (!doesColumnExist(db, "plants", "PlantedStart")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PlantedStart INTEGER")
        }
        if (!doesColumnExist(db, "plants", "PlantedEnd")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PlantedEnd INTEGER")
        }
        if (!doesColumnExist(db, "plants", "PollinateDate")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PollinateDate INTEGER")
        }
        if (!doesColumnExist(db, "plants", "SeedsPlanted")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN SeedsPlanted INTEGER")
        }
        if (!doesColumnExist(db, "plants", "SeedsHarvest")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN SeedsHarvest INTEGER")
        }
        if (!doesColumnExist(db, "plants", "Stamp")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Stamp INTEGER")
        }
        if (!doesColumnExist(db, "plants", "PlantStatus")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PlantStatus TEXT")
        }
        if (!doesColumnExist(db, "plants", "StatusNote")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN StatusNote TEXT")
        }
        if (!doesColumnExist(db, "plants", "StockPrice")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN StockPrice INTEGER")
        }
        if (!doesColumnExist(db, "plants", "PurchasePrice")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PurchasePrice INTEGER")
        }
        if (!doesColumnExist(db, "plants", "TotalValue")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN TotalValue INTEGER")
        }
        if (!doesColumnExist(db, "plants", "USD")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN USD INTEGER")
        }
        if (!doesColumnExist(db, "plants", "EUR")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN EUR INTEGER")
        }
        if (!doesColumnExist(db, "plants", "Photo1")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Photo1 TEXT")
        }
        if (!doesColumnExist(db, "plants", "Photo2")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Photo2 TEXT")
        }
        if (!doesColumnExist(db, "plants", "Photo3")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Photo3 TEXT")
        }
        if (!doesColumnExist(db, "plants", "Photo4")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Photo4 TEXT")
        }
        if (!doesColumnExist(db, "plants", "PhotoLink1")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PhotoLink1 TEXT")
        }
        if (!doesColumnExist(db, "plants", "PhotoLink2")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PhotoLink2 TEXT")
        }
        if (!doesColumnExist(db, "plants", "PhotoLink3")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PhotoLink3 TEXT")
        }
        if (!doesColumnExist(db, "plants", "PhotoLink4")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN PhotoLink4 TEXT")
        }
        if (!doesColumnExist(db, "plants", "AddedBy")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN AddedBy TEXT")
        }
        if (!doesColumnExist(db, "plants", "LastEditedBy")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN LastEditedBy TEXT")
        }
        if (!doesColumnExist(db, "plants", "Weight")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Weight TEXT")
        }
        if (!doesColumnExist(db, "plants", "Grams")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Grams INTEGER")
        }
        if (!doesColumnExist(db, "plants", "TraySize")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN TraySize TEXT")
        }
        if (!doesColumnExist(db, "plants", "TrayQty")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN TrayQty INTEGER")
        }
        if (!doesColumnExist(db, "plants", "Variegated")) {
            db.execSQL("ALTER TABLE plants ADD COLUMN Variegated INTEGER")
        }
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the new 'website' table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `website` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `StockID` INTEGER NOT NULL,
                `ShopifyID` INTEGER,
                `ShopifySKU` TEXT,
                `NameConcat` TEXT NOT NULL,
                `StockQty` INTEGER NOT NULL,
                `StockPrice` REAL NOT NULL,
                `PlantDescription` TEXT NOT NULL,
                `Photo1` TEXT,
                `Photo2` TEXT,
                `Photo3` TEXT,
                `Photo4` TEXT,
                `CreatedAt` TEXT,
                `UpdatedAt` TEXT
            )
            """.trimIndent()
        )
    }
}
