package com.example.rrhe

import okhttp3.ConnectionPool
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder
import java.util.Date

object ApiClient {

    private fun getClient(isEmulator: Boolean): Retrofit {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }

        val okHttpClientBuilder = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .retryOnConnectionFailure(false)
            .connectTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        okHttpClientBuilder.addInterceptor(logging)
        okHttpClientBuilder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept-Encoding", "identity")
                .addHeader("Connection", "close")
                .build()
            chain.proceed(request)
        }
        okHttpClientBuilder.connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))

        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeConverter())
            .create()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.getBaseUrl(isEmulator))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClientBuilder.build())
            .build()
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")
                || "google_sdk" == android.os.Build.PRODUCT)
    }

    val apiService: ApiService by lazy {
        getClient(isEmulator()).create(ApiService::class.java)
    }
}