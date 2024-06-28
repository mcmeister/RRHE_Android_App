package com.example.rrhe

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Network {
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
