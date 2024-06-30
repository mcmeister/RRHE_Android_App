package com.example.rrhe

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "plants")
@TypeConverters(DateTypeConverter::class)
data class Plant(
    @PrimaryKey val StockID: Int,
    val NameConcat: String?,
    val Family: String?,
    val Species: String?,
    val Subspecies: String? = "",  // Default value for empty subspecies
    val StockQty: Int,
    val StockPrice: Double,
    val PlantDescription: String?,
    val PhotoLink1: String?,
    val Stamp: Date
) : Parcelable {

    fun ensureNonNullValues(): Plant {
        return this.copy(
            Family = this.Family?.ifBlank { "Unknown" } ?: "Unknown",
            Species = this.Species?.ifBlank { "Unknown" } ?: "Unknown",
            Subspecies = this.Subspecies?.ifBlank { "" } ?: "",
            PlantDescription = this.PlantDescription?.ifBlank { "No description" } ?: "No description",
            PhotoLink1 = this.PhotoLink1?.ifBlank { "No photo" } ?: "No photo"
        )
    }
}