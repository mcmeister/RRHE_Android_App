package com.example.rrhe

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey var StockID: Int?,
    var M_ID: Int?,
    var F_ID: Int?,
    var Family: String?,
    var Species: String?,
    var Subspecies: String?,
    var ThaiName: String?,
    var NameConcat: String?,
    var TableName: String?,
    var StockQty: Int,
    var StockPrice: Int?,
    var Mother: Int?,
    var Website: Int?,
    var PlantedStart: String?,
    var PlantedEnd: String?,
    var PollinateDate: String?,
    var SeedsPlanted: String?,
    var SeedsHarvest: String?,
    var Stamp: String?,
    var PlantStatus: String?,
    var PlantDescription: String?,
    var StatusNote: String?,
    var PurchasePrice: Int?,
    var TotalValue: Int?,
    var USD: Int?,
    var EUR: Int?,
    var Photo1: String?,
    var Photo2: String?,
    var Photo3: String?,
    var Photo4: String?,
    var PhotoLink1: String?,
    var PhotoLink2: String?,
    var PhotoLink3: String?,
    var PhotoLink4: String?,
    var AddedBy: String?,
    var LastEditedBy: String?,
    var Weight: String?,
    var Grams: Int?,
    var TraySize: String?,
    var TrayQty: Int?,
    var Variegated: Int?  // Change this to Int
) : Parcelable {
    fun ensureNonNullValues(): Plant {
        return this.copy(
            Family = this.Family?.ifBlank { "" } ?: "",
            Species = this.Species?.ifBlank { "" } ?: "",
            Subspecies = this.Subspecies?.ifBlank { "" } ?: "",
            ThaiName = this.ThaiName?.ifBlank { "" } ?: "",
            NameConcat = this.NameConcat?.ifBlank { "" } ?: "",
            TableName = this.TableName?.ifBlank { "" } ?: "",
            PlantDescription = this.PlantDescription?.ifBlank { "" } ?: "",
            PlantStatus = this.PlantStatus?.ifBlank { "" } ?: "",
            StatusNote = this.StatusNote?.ifBlank { "" } ?: "",
            PollinateDate = this.PollinateDate,
            SeedsPlanted = this.SeedsPlanted,
            SeedsHarvest = this.SeedsHarvest,
            PlantedStart = this.PlantedStart,
            PlantedEnd = this.PlantedEnd,
            Photo1 = this.Photo1?.ifBlank { "" } ?: "",
            Photo2 = this.Photo2?.ifBlank { "" } ?: "",
            Photo3 = this.Photo3?.ifBlank { "" } ?: "",
            Photo4 = this.Photo4?.ifBlank { "" } ?: "",
            PhotoLink1 = this.PhotoLink1?.ifBlank { "" } ?: "",
            PhotoLink2 = this.PhotoLink2?.ifBlank { "" } ?: "",
            PhotoLink3 = this.PhotoLink3?.ifBlank { "" } ?: "",
            PhotoLink4 = this.PhotoLink4?.ifBlank { "" } ?: "",
            AddedBy = this.AddedBy?.ifBlank { "" } ?: "",
            LastEditedBy = this.LastEditedBy?.ifBlank { "" } ?: "",
            Weight = this.Weight?.ifBlank { "" } ?: "",
            TraySize = this.TraySize?.ifBlank { "" } ?: "",
            StockPrice = this.StockPrice ?: 0,
            PurchasePrice = this.PurchasePrice ?: 0,
            TotalValue = this.TotalValue ?: 0,
            USD = this.USD ?: 0,
            EUR = this.EUR ?: 0,
            Grams = this.Grams ?: 0,
            TrayQty = this.TrayQty ?: 0,
            Variegated = this.Variegated ?: 0  // Change this to default to 0
        )
    }
}
