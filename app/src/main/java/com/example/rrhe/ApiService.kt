package com.example.rrhe

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("rrhe")
    fun getRRHE(): Call<List<Plant>>

    @POST("rrhe/update")
    fun updatePlant(@Body plant: PlantUpdateRequest): Call<ResponseBody>
}
