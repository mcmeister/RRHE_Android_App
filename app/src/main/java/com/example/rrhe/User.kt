package com.example.rrhe

data class User(
    val userId: Int,       // Unique ID for the user
    val userName: String,  // Username
    val langId: String,    // Language ID
    val fcmToken: String?  // FCM token (optional, since it can be null initially)
)