package com.example.rrhe

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "stats")
data class Stats(
    @PrimaryKey val id: Int = 0,
    @SerializedName("EUR") val eur: Double,
    @SerializedName("MValue") val mValue: Double,
    @SerializedName("NonMValue") val nonMValue: Double,
    @SerializedName("TotalM") val totalM: Int,
    @SerializedName("TotalNonM") val totalNonM: Int,
    @SerializedName("TotalPlants") val totalPlants: Int,
    @SerializedName("TotalRows") val totalRows: Int,
    @SerializedName("TotalValue") val totalValue: Double,
    @SerializedName("USD") val usd: Double,
    @SerializedName("WebPlants") val webPlants: Int,
    @SerializedName("WebQty") val webQty: Int,
    @SerializedName("WebValue") val webValue: Double
)
