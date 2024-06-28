package com.example.rrhe

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Plant(
    val StockID: Int,
    val NameConcat: String,
    val StockQty: Int,
    val PhotoLink1: String
) : Parcelable
