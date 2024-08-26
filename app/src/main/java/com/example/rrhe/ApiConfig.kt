package com.example.rrhe

object ApiConfig {
    // Base URLs for main database server and HTTP server
    private const val MAIN_DATABASE_BASE_URL = "http://192.168.1.110:5000/"
    private const val EMULATOR_MAIN_DATABASE_BASE_URL = "http://10.0.2.2:5000/"
    private const val HTTP_SERVER_BASE_URL = "http://192.168.1.110:8000/"

    // Function to get the correct Main Database Base URL based on the environment
    fun getBaseUrl(isEmulator: Boolean): String {
        return if (isEmulator) EMULATOR_MAIN_DATABASE_BASE_URL else MAIN_DATABASE_BASE_URL
    }

    // Function to get the HTTP Server Base URL
    fun getHttpServerBaseUrl(): String {
        return HTTP_SERVER_BASE_URL
    }
}
