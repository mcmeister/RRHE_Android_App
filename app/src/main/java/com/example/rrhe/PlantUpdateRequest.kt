package com.example.rrhe

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlantUpdateRequest(
    val StockID: Int?,
    val M_ID: Int?,
    val F_ID: Int?,
    val Family: String?,
    val Species: String?,
    val Subspecies: String? = "",
    val ThaiName: String?,
    val NameConcat: String?,
    val TableName: String?,
    val StockQty: Int,
    val StockPrice: Int?,
    val Mother: Int?,
    val Website: Int?,
    val PlantedStart: String?,
    val PlantedEnd: String?,
    val PollinateDate: String?,
    val SeedsPlanted: String?,
    val SeedsHarvest: String?,
    val PlantStatus: String?,
    val Stamp: String?,
    val PlantDescription: String?,
    val StatusNote: String?,
    val PurchasePrice: Int?,
    val TotalValue: Int?,
    val USD: Int?,
    val EUR: Int?,
    val Photo1: String?,
    val Photo2: String?,
    val Photo3: String?,
    val Photo4: String?,
    val PhotoLink1: String?,
    val PhotoLink2: String?,
    val PhotoLink3: String?,
    val PhotoLink4: String?,
    val AddedBy: String?,
    val LastEditedBy: String?,
    val Weight: String?,
    val Grams: Int?,
    val TraySize: String?,
    val TrayQty: Int?,
    val Variegated: Int?
) : Parcelable {
    companion object {
        fun fromPlant(plant: Plant): PlantUpdateRequest {
            return PlantUpdateRequest(
                StockID = plant.StockID,
                M_ID = plant.M_ID,
                F_ID = plant.F_ID,
                Family = plant.Family,
                Species = plant.Species,
                Subspecies = plant.Subspecies,
                ThaiName = plant.ThaiName,
                NameConcat = plant.NameConcat,
                TableName = plant.TableName,
                StockQty = plant.StockQty,
                StockPrice = plant.StockPrice,
                Mother = plant.Mother,
                Website = plant.Website,
                PlantedStart = plant.PlantedStart,
                PlantedEnd = plant.PlantedEnd,
                PollinateDate = plant.PollinateDate,
                SeedsPlanted = plant.SeedsPlanted,
                SeedsHarvest = plant.SeedsHarvest,
                PlantStatus = plant.PlantStatus,
                Stamp = plant.Stamp,
                PlantDescription = plant.PlantDescription,
                StatusNote = plant.StatusNote,
                PurchasePrice = plant.PurchasePrice,
                TotalValue = plant.TotalValue,
                USD = plant.USD,
                EUR = plant.EUR,
                Photo1 = plant.Photo1,
                Photo2 = plant.Photo2,
                Photo3 = plant.Photo3,
                Photo4 = plant.Photo4,
                PhotoLink1 = plant.PhotoLink1,
                PhotoLink2 = plant.PhotoLink2,
                PhotoLink3 = plant.PhotoLink3,
                PhotoLink4 = plant.PhotoLink4,
                AddedBy = plant.AddedBy,
                LastEditedBy = plant.LastEditedBy,
                Weight = plant.Weight,
                Grams = plant.Grams,
                TraySize = plant.TraySize,
                TrayQty = plant.TrayQty,
                Variegated = plant.Variegated
            )
        }
    }
}
