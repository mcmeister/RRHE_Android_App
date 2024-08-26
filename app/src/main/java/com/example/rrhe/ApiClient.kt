package com.example.rrhe

import com.google.gson.GsonBuilder
import okhttp3.ConnectionPool
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object ApiClient {

    private val okHttpClientBuilder = OkHttpClient.Builder()
    private lateinit var okHttpClient: OkHttpClient

    private fun createClient(baseUrl: String): Retrofit {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }

        okHttpClientBuilder
            .cookieJar(JavaNetCookieJar(cookieManager))
            .retryOnConnectionFailure(true)  // Allow retries on connection failures
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        val logging = HttpLoggingInterceptor().apply {
            // You can adjust this to HttpLoggingInterceptor.Level.BODY for more detailed logs
            level = HttpLoggingInterceptor.Level.BASIC
        }
        okHttpClientBuilder.addInterceptor(logging)
        okHttpClientBuilder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept-Encoding", "identity")
                // Removed Connection: close header to allow connection reuse
                .build()
            chain.proceed(request)
        }
        okHttpClientBuilder.addInterceptor(RetryInterceptor())
        okHttpClientBuilder.connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))  // Increased connection pool timeout

        okHttpClient = okHttpClientBuilder.build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    // Regular instance method for use in the app
    private fun getClient(baseUrl: String): Retrofit {
        return createClient(baseUrl)
    }

    // Static method for use in MyFirebaseMessagingService
    @JvmStatic
    fun getStaticClient(baseUrl: String): Retrofit {
        return createClient(baseUrl)
    }

    // Main API service
    val apiService: ApiService by lazy {
        getClient(ApiConfig.getBaseUrl(isEmulator())).create(ApiService::class.java)
    }

    // HTTP Server API service for photo uploads
    val httpServerApiService: ApiService by lazy {
        getClient(ApiConfig.getHttpServerBaseUrl()).create(ApiService::class.java)
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

    // Static method for use in MyFirebaseMessagingService
    @JvmStatic
    fun isStaticEmulator(): Boolean {
        return isEmulator()
    }

    fun closeAllConnections() {
        okHttpClient.connectionPool.evictAll()
        okHttpClient.dispatcher.cancelAll()
    }
}

class RetryInterceptor : okhttp3.Interceptor {
    private val maxRetries = 1
    private val retryDelayMillis = 1000L

    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        var response: okhttp3.Response? = null
        var attempts = 0

        while (attempts < maxRetries) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                if (attempts >= maxRetries - 1) {
                    response?.close()
                    throw e
                }
                response?.close()
                Thread.sleep(retryDelayMillis)
            }
            attempts++
        }

        return response ?: chain.proceed(request)
    }
}
