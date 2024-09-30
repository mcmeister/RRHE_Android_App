package com.example.rrhe

import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import java.text.SimpleDateFormat
import java.util.*

class SearchBar(
    private val searchBarLayout: LinearLayout,
    private val searchEditText: EditText,
    private val searchIcon: ImageView,
    qrCodeButton: ImageView,
    clearSearchButton: ImageView,
    private val motherFilterButton: ImageView?,
    private val websiteFilterButton: ImageView?,
    private val onSearch: (String, Boolean, Boolean) -> Unit
) {
    private var filterMother = false
    private var filterWebsite = false

    init {
        qrCodeButton.setOnClickListener {
            Log.d("SearchBar", "QR Code button clicked")
        }

        searchIcon.setOnClickListener {
            searchBarLayout.visibility = View.VISIBLE
            searchIcon.visibility = View.GONE
        }

        clearSearchButton.setOnClickListener {
            clearSearch()
        }

        searchEditText.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().trim()
            val interpretedQuery = interpretQuery(query)
            onSearch(interpretedQuery, filterMother, filterWebsite)
        }

        motherFilterButton?.setOnClickListener {
            filterMother = !filterMother
            updateFilterButtonColors()
            onSearch(searchEditText.text.toString(), filterMother, filterWebsite)
        }

        websiteFilterButton?.setOnClickListener {
            filterWebsite = !filterWebsite
            updateFilterButtonColors()
            onSearch(searchEditText.text.toString(), filterMother, filterWebsite)
        }

        searchBarLayout.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchBarLayout.visibility = View.GONE
                searchIcon.visibility = View.VISIBLE
            }
        }
    }

    private fun interpretQuery(query: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        return when (query.lowercase()) {
            "today" -> dateFormat.format(calendar.time)
            "yesterday" -> {
                calendar.add(Calendar.DATE, -1)
                dateFormat.format(calendar.time)
            }
            "tomorrow" -> {
                calendar.add(Calendar.DATE, 1)
                dateFormat.format(calendar.time)
            }
            else -> query
        }
    }

    fun clearSearch() {
        searchEditText.text.clear()
        filterMother = false
        filterWebsite = false
        resetFilterButtonColors()
        onSearch("", filterMother, filterWebsite)
        searchBarLayout.visibility = View.GONE
        searchIcon.visibility = View.VISIBLE
    }

    fun setSearchText(stockId: String) {
        searchEditText.setText(stockId)
        onSearch(stockId, filterMother, filterWebsite)
    }

    private fun updateFilterButtonColors() {
        val activeColor = ContextCompat.getColor(searchBarLayout.context, R.color.filter_active)
        val inactiveColor = ContextCompat.getColor(searchBarLayout.context, R.color.filter_inactive)

        motherFilterButton?.setColorFilter(if (filterMother) activeColor else inactiveColor)
        websiteFilterButton?.setColorFilter(if (filterWebsite) activeColor else inactiveColor)
    }

    private fun resetFilterButtonColors() {
        val blackColor = ContextCompat.getColor(searchBarLayout.context, android.R.color.black)
        motherFilterButton?.setColorFilter(blackColor)
        websiteFilterButton?.setColorFilter(blackColor)
    }
}
