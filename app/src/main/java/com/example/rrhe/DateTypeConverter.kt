package com.example.rrhe

import androidx.room.TypeConverter
import com.google.gson.*
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateTypeConverter : JsonDeserializer<Date>, JsonSerializer<Date> {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date? {
        return try {
            dateFormat.parse(json.asString)
        } catch (e: Exception) {
            null
        }
    }

    override fun serialize(src: Date?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src?.let { dateFormat.format(it) } ?: "")
    }
}