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
) : Parcelable
