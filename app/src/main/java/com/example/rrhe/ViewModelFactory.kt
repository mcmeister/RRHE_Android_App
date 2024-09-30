package com.example.rrhe

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Factory class to create instances of ViewModels with the required application
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(StockViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                StockViewModel(application) as T
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                StatsViewModel() as T
            }
            modelClass.isAssignableFrom(WebsiteViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                WebsiteViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
