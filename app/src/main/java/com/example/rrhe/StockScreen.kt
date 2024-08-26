package com.example.rrhe

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rrhe.ui.theme.RRHETheme

class StockScreen : AppCompatActivity() {

    private lateinit var inactivityDetector: InactivityDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inactivityDetector = InactivityDetector(this)
        setContent {
            val viewModel: StockViewModel = viewModel()
            StockScreenComposable(viewModel, inactivityDetector)  // Pass inactivityDetector to Composable
        }
    }

    override fun onResume() {
        super.onResume()
        inactivityDetector.reset()
    }

    override fun onPause() {
        super.onPause()
        inactivityDetector.stop()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        inactivityDetector.reset() // Reset inactivity timer on user interaction
        return super.onTouchEvent(event)
    }
}

@Composable
fun StockScreenComposable(viewModel: StockViewModel = viewModel(), inactivityDetector: InactivityDetector) {
    RRHETheme {
        val plants by viewModel.plants.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.syncWithMainDatabase()
        }

        val plantDetailsLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    viewModel.updatePlantList(PlantRepository.plants.value ?: emptyList())
                }
            }

        AndroidView(factory = { ctx ->
            // Use the correct parent for the layout inflation
            val inflater = LayoutInflater.from(ctx)
            val parent = LinearLayout(ctx) // Creating a temporary parent for layout params resolution
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

            // Initialize SearchBar functionality
            SearchBar(
                searchBarLayout = searchBarLayout,
                searchEditText = searchEditText,
                searchIcon = searchIcon,
                qrCodeButton = qrCodeButton,
                clearSearchButton = clearSearchButton,
                motherFilterButton = motherFilterButton,
                websiteFilterButton = websiteFilterButton,
                onSearch = { query, filterMother, filterWebsite ->
                    viewModel.updateSearchQuery(query, filterMother, filterWebsite)
                    inactivityDetector.reset() // Reset inactivity timer on search interaction
                }
            )

            // Set layout manager and adapter for the plant list
            plantList.layoutManager = LinearLayoutManager(view.context)
            val adapter = PlantAdapter(plants, inactivityDetector)
            plantList.adapter = adapter
            adapter.updatePlants(plants)

            // Scroll listener to hide/show search icon and FAB
            plantList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        // Scrolling down
                        searchIcon.visibility = View.GONE
                        fabAddNewPlant.hide()
                    } else if (dy < 0) {
                        // Scrolling up
                        if (searchBarLayout.visibility == View.GONE) {
                            // Ensure the search icon is visible when scrolling up
                            searchIcon.visibility = View.VISIBLE
                        }
                        fabAddNewPlant.show()
                    }
                }
            })

            clearSearchButton.setOnClickListener {
                searchBarLayout.visibility = View.GONE
                searchIcon.visibility = View.VISIBLE // Show the icon when search bar is closed
            }

            // FAB click listener
            fabAddNewPlant.setOnClickListener {
                val intent = Intent(context, NewPlantActivity::class.java)
                context.startActivity(intent)
                inactivityDetector.reset() // Reset inactivity timer on FAB click
            }

            adapter.setOnItemClickListener { plant ->
                val intent = Intent(context, PlantDetailsActivity::class.java).apply {
                    putExtra("plant", plant)
                }
                plantDetailsLauncher.launch(intent)
                inactivityDetector.reset() // Reset inactivity timer on plant item click
            }
        })
    }
}