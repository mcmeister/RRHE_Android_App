package com.example.rrhe

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("rrhe")
    suspend fun getRRHE(): List<Plant>

    @GET("rrhe/changes")
    suspend fun getRRHEChanges(@Query("last_sync_time") lastSyncTime: String): List<Plant>

    @POST("rrhe/update")
    suspend fun updatePlant(@Body plant: PlantUpdateRequest): ResponseBody

    @GET("stats")
    suspend fun getStats(): List<Stats>

    @POST("upload")
    suspend fun uploadPhoto(@Body photo: RequestBody): Response<ResponseBody>

    @POST("/delete_photo")
    suspend fun deletePhoto(@Body request: Map<String, String>): Response<Void>

    @POST("rrhe/insert")
    suspend fun insertNewPlant(@Body plant: Plant): Plant

    @POST("/login")
    fun login(@Body loginData: Map<String, String>): Call<User>

    @POST("/update_fcm_token")
    fun updateFcmToken(@Body tokenData: Map<String, String>): Call<Void>
}
