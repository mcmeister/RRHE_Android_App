package com.example.rrhe

import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.core.content.ContextCompat

class SearchBar(
    private val searchBarLayout: LinearLayout,
    private val searchEditText: EditText,
    private val searchIcon: ImageView,
    qrCodeButton: ImageView,
    clearSearchButton: ImageView,
    private val motherFilterButton: ImageView,
    private val websiteFilterButton: ImageView,
    private val onSearch: (String, Boolean, Boolean) -> Unit
) {
    private var filterMother = false
    private var filterWebsite = false

    init {
        Log.d("SearchBar", "Initializing SearchBar")

        searchIcon.setOnClickListener {
            searchBarLayout.visibility = View.VISIBLE
            searchIcon.visibility = View.GONE
            Log.d("SearchBar", "SearchBar visible")
        }

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()  // Clear the EditText input
            filterMother = false
            filterWebsite = false
            updateFilterButtonColors()

            // Clear the search query and reset filters in the ViewModel
            onSearch("", filterMother, filterWebsite)

            searchBarLayout.visibility = View.GONE
            searchIcon.visibility = View.VISIBLE
            Log.d("SearchBar", "SearchBar hidden and query cleared")
        }

        searchEditText.doOnTextChanged { text, _, _, _ ->
            Log.d("SearchBar", "Search query: $text")
            val query = text.toString().trim()
            onSearch(query, filterMother, filterWebsite)
        }

        qrCodeButton.setOnClickListener {
            // QR code scanning functionality will be added later
        }

        motherFilterButton.setOnClickListener {
            filterMother = !filterMother
            updateFilterButtonColors()
            Log.d("SearchBar", "Mother filter: $filterMother")
            onSearch(searchEditText.text.toString(), filterMother, filterWebsite)
        }

        websiteFilterButton.setOnClickListener {
            filterWebsite = !filterWebsite
            updateFilterButtonColors()
            Log.d("SearchBar", "Website filter: $filterWebsite")
            onSearch(searchEditText.text.toString(), filterMother, filterWebsite)
        }

        searchBarLayout.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchBarLayout.visibility = View.GONE
                searchIcon.visibility = View.VISIBLE
                Log.d("SearchBar", "SearchBar lost focus and hidden")
            }
        }
    }

    private fun updateFilterButtonColors() {
        val activeColor = ContextCompat.getColor(searchBarLayout.context, R.color.filter_active)
        val inactiveColor = ContextCompat.getColor(searchBarLayout.context, R.color.filter_inactive)

        motherFilterButton.setColorFilter(if (filterMother) activeColor else inactiveColor)
        websiteFilterButton.setColorFilter(if (filterWebsite) activeColor else inactiveColor)
    }
}
