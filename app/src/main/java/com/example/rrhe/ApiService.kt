package com.example.rrhe

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("rrhe")
    fun getRRHE(): Call<List<Plant>>
}
