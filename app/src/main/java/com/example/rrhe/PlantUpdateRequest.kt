package com.example.rrhe

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlantUpdateRequest(
    val StockID: Int,
    val Family: String,
    val Species: String,
    val Subspecies: String,
    val StockQty: Int,
    val StockPrice: Double,
    val PlantDescription: String
) : Parcelable {
    companion object {
        fun fromPlant(plant: Plant): PlantUpdateRequest {
            return PlantUpdateRequest(
                StockID = plant.StockID,
                Family = plant.Family ?: "Unknown",
                Species = plant.Species ?: "Unknown",
                Subspecies = plant.Subspecies ?: "",
                StockQty = plant.StockQty,
                StockPrice = plant.StockPrice,
                PlantDescription = plant.PlantDescription ?: "No description"
            )
        }
    }
}
