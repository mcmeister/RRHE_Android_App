package com.example.rrhe

import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged

class SearchBar(
    private val searchBarLayout: LinearLayout,
    private val searchEditText: EditText,
    private val searchIcon: ImageView,
    qrCodeButton: ImageView,
    clearSearchButton: ImageView,
    private val onSearch: (String) -> Unit
) {
    init {
        Log.d("SearchBar", "Initializing SearchBar")

        searchIcon.setOnClickListener {
            searchBarLayout.visibility = View.VISIBLE
            searchIcon.visibility = View.GONE
            Log.d("SearchBar", "SearchBar visible")
        }

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            searchBarLayout.visibility = View.GONE
            searchIcon.visibility = View.VISIBLE
            Log.d("SearchBar", "SearchBar hidden")
        }

        searchEditText.doOnTextChanged { text, _, _, _ ->
            Log.d("SearchBar", "Search query: $text")
            onSearch(text.toString())
        }

        qrCodeButton.setOnClickListener {
            // QR code scanning functionality will be added later
        }

        searchBarLayout.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchBarLayout.visibility = View.GONE
                searchIcon.visibility = View.VISIBLE
                Log.d("SearchBar", "SearchBar lost focus and hidden")
            }
        }
    }
}
