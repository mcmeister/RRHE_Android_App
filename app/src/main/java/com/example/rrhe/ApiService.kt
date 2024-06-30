package com.example.rrhe

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Date

interface ApiService {
    @GET("rrhe")
    suspend fun getRRHE(): List<Plant>

    @GET("rrhe/changes")
    suspend fun getRRHEChanges(@Query("last_sync_time") lastSyncTime: String): List<Plant>

    @POST("rrhe/update")
    suspend fun updatePlant(@Body plant: PlantUpdateRequest): ResponseBody

    @GET("stats")
    suspend fun getStats(): Stats
}