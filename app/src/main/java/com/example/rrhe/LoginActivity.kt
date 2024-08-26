package com.example.rrhe

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rrhe.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val predefinedLogin = "Test"
    private val predefinedPassword = "test"

    private fun saveLoginDetails(login: String, password: String) {
        val sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("login", login)
            putString("password", password)
            apply()
        }
    }

    private fun loadLoginDetails() {
        val sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        binding.loginEditText.setText(sharedPref.getString("login", ""))
        binding.passwordEditText.setText(sharedPref.getString("password", ""))
        binding.savePasswordCheckbox.isChecked = sharedPref.contains("login")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadLoginDetails() // Load saved login details if available

        binding.loginButton.setOnClickListener {
            val login = binding.loginEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            if (login == predefinedLogin && password == predefinedPassword) {
                if (binding.savePasswordCheckbox.isChecked) {
                    saveLoginDetails(login, password)
                }
                InactivityDetector(this@LoginActivity).reset()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid login credentials", Toast.LENGTH_SHORT).show()
            }
        }

        // Add TextWatchers to reset inactivity timer when user types
        binding.loginEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                InactivityDetector(this@LoginActivity).reset() // Reset inactivity timer when user types
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                InactivityDetector(this@LoginActivity).reset() // Reset inactivity timer when user types
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        InactivityDetector(this).reset() // Start/reset the inactivity detector when the activity resumes
    }

    override fun onPause() {
        super.onPause()
        InactivityDetector(this).stop() // Stop the inactivity detector when the activity pauses
    }
}
