package com.example.rrhe

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import com.example.rrhe.ui.theme.RRHETheme

class StockScreen : AppCompatActivity() {

    // private lateinit var inactivityDetector: InactivityDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // inactivityDetector = InactivityDetector(this)
        setContent {
            val viewModel: StockViewModel = viewModel()
            StockScreenComposable(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // inactivityDetector.reset()
    }

    override fun onPause() {
        super.onPause()
        // inactivityDetector.stop()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // inactivityDetector.reset()
        return super.onTouchEvent(event)
    }
}

@Composable
fun StockScreenComposable(viewModel: StockViewModel = viewModel()) {
    RRHETheme {
        val plants by viewModel.plants.collectAsState()
        val context = LocalContext.current

        var searchBar: SearchBar? = null

        // Get the colors from the theme
        val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
        val textColor = MaterialTheme.colorScheme.onBackground.toArgb()

        val plantDetailsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                viewModel.updatePlantList(PlantRepository.plants.value ?: emptyList())
            }
        }

        val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val scannedStockId = result.data?.getStringExtra("scannedStockId")
                if (!scannedStockId.isNullOrEmpty()) {
                    searchBar?.setSearchText(scannedStockId)
                }
            }
        }

        AndroidView(factory = { ctx ->
            val inflater = LayoutInflater.from(ctx)
            val parent = LinearLayout(ctx)
            inflater.inflate(R.layout.screen_stock, parent, false)
        }, update = { view ->
            val searchIcon = view.findViewById<ImageView>(R.id.searchIcon)
            val searchBarLayout = view.findViewById<LinearLayout>(R.id.searchBarLayout)
            val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
            val qrCodeButton = view.findViewById<ImageView>(R.id.qrCodeButton)
            val clearSearchButton = view.findViewById<ImageView>(R.id.clearSearchButton)
            val motherFilterButton = view.findViewById<ImageView>(R.id.motherFilterButton)
            val websiteFilterButton = view.findViewById<ImageView>(R.id.websiteFilterButton)
            val plantList = view.findViewById<RecyclerView>(R.id.plantList)
            val fabAddNewPlant = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_new_plant)

            searchBar = SearchBar(
                searchBarLayout = searchBarLayout,
                searchEditText = searchEditText,
                searchIcon = searchIcon,
                qrCodeButton = qrCodeButton,
                clearSearchButton = clearSearchButton,
                motherFilterButton = motherFilterButton,
                websiteFilterButton = websiteFilterButton,
                onSearch = { query, filterMother, filterWebsite ->
                    viewModel.updateSearchQuery(query, filterMother, filterWebsite)
                    // inactivityDetector.reset()
                }
            )

            // Apply theme-based colors outside the @Composable scope
            searchEditText.setBackgroundColor(backgroundColor)
            searchEditText.setTextColor(textColor)

            // Pass the textColor to the PlantAdapter
            val adapter = PlantAdapter(plants, textColor)
            plantList.adapter = adapter

            plantList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        searchIcon.visibility = View.GONE
                        fabAddNewPlant.hide()
                    } else if (dy < 0) {
                        if (searchBarLayout.visibility == View.GONE) {
                            searchIcon.visibility = View.VISIBLE
                        }
                        fabAddNewPlant.show()
                    }
                }
            })

            clearSearchButton.setOnClickListener {
                searchBar?.clearSearch()
                // inactivityDetector.reset()
            }

            qrCodeButton.setOnClickListener {
                val intent = Intent(context, QRScannerActivity::class.java)
                qrScannerLauncher.launch(intent)
            }

            fabAddNewPlant.setOnClickListener {
                val intent = Intent(context, NewPlantActivity::class.java)
                context.startActivity(intent)
                // inactivityDetector.reset()
            }

            adapter.setOnItemClickListener { plant ->
                val intent = Intent(context, PlantDetailsActivity::class.java).apply {
                    putExtra("plant", plant as Parcelable)
                }
                plantDetailsLauncher.launch(intent)
                // inactivityDetector.reset()
            }

            plantList.layoutManager = LinearLayoutManager(view.context)
            adapter.updatePlants(plants)
        })
    }
}