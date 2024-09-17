package com.example.rrhe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.rrhe.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved login details if available
        loadLoginDetails()

        binding.loginButton.setOnClickListener {
            val login = binding.loginEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            loginUser(login, password)
        }
    }

    private fun loginUser(userName: String, password: String) {
        // Simulate successful login, normally you'd call an API here
        if (binding.savePasswordCheckbox.isChecked) {
            saveLoginDetails(userName, password)
        }

        // Save the username in SharedPreferences
        saveUserName(userName)

        // Send stored token to the server
        MyFirebaseMessagingService.sendStoredToken(this)

        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        finish()
    }

    private fun saveUserName(userName: String) {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("userName", userName).apply()
    }

    private fun saveLoginDetails(userName: String, password: String) {
        // Create or retrieve the MasterKey
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Create or access the EncryptedSharedPreferences
        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "LoginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        with(sharedPreferences.edit()) {
            putString("login", userName)
            putString("password", password)
            apply()
        }
    }

    private fun loadLoginDetails() {
        // Create or retrieve the MasterKey
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Create or access the EncryptedSharedPreferences
        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "LoginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val savedLogin = sharedPreferences.getString("login", "")
        val savedPassword = sharedPreferences.getString("password", "")

        binding.loginEditText.setText(savedLogin)
        binding.passwordEditText.setText(savedPassword)
        binding.savePasswordCheckbox.isChecked = sharedPreferences.contains("login")
    }
}