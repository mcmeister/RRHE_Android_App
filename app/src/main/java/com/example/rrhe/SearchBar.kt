package com.example.rrhe

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged

class SearchBar(
    private val searchBarLayout: LinearLayout,
    private val searchEditText: EditText,
    private val searchIcon: ImageView,
    private val qrCodeButton: ImageView,
    private val clearSearchButton: ImageView,
    private val onSearch: (String) -> Unit
) {
    init {
        searchIcon.setOnClickListener {
            searchBarLayout.visibility = View.VISIBLE
            searchIcon.visibility = View.GONE
        }

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            searchBarLayout.visibility = View.GONE
            searchIcon.visibility = View.VISIBLE
        }

        searchEditText.doOnTextChanged { text, _, _, _ ->
            onSearch(text.toString())
        }

        qrCodeButton.setOnClickListener {
            // QR code scanning functionality will be added later
        }

        searchBarLayout.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchBarLayout.visibility = View.GONE
                searchIcon.visibility = View.VISIBLE
            }
        }
    }
}
