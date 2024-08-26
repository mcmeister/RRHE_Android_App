package com.example.rrhe

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PairTypeConverter {
    private fun getDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    @TypeConverter
    @JvmStatic
    fun fromDatePair(pair: Pair<Date, Date>?): String? {
        val dateFormat = getDateFormat()
        return pair?.let {
            "${dateFormat.format(it.first)},${dateFormat.format(it.second)}"
        }
    }

    @TypeConverter
    @JvmStatic
    fun toDatePair(data: String?): Pair<Date, Date>? {
        val dateFormat = getDateFormat()
        return data?.split(",")?.let {
            if (it.size == 2) {
                val firstDate = dateFormat.parse(it[0])
                val secondDate = dateFormat.parse(it[1])
                if (firstDate != null && secondDate != null) {
                    Pair(firstDate, secondDate)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
}
