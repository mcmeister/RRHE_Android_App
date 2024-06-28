package com.example.rrhe

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Plant(
    val StockID: Int,
    val NameConcat: String,
    val Family: String,
    val Species: String,
    val Subspecies: String,
    val StockQty: Int,
    val StockPrice: Double,
    val PlantDescription: String,
    val PhotoLink1: String
) : Parcelable
