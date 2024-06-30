package com.example.rrhe

object ApiConfig {
    // Base URLs for actual device and emulator
    private const val DEVICE_BASE_URL = "http://192.168.1.110:5000/"
    private const val EMULATOR_BASE_URL = "http://10.0.2.2:5000/"

    // Function to get the correct Base URL based on the environment
    fun getBaseUrl(isEmulator: Boolean): String {
        return if (isEmulator) EMULATOR_BASE_URL else DEVICE_BASE_URL
    }
}
