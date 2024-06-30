package com.example.rrhe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StockScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: StockViewModel = viewModel()
            StockScreenComposable(viewModel)
        }
    }
}

@Composable
fun StockScreenComposable(viewModel: StockViewModel = viewModel()) {
    val plants by viewModel.plants.collectAsState()
    val plantDetailsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            viewModel.updatePlantList(PlantRepository.plants.value ?: emptyList())
        }
    }

    AndroidView(factory = { ctx ->
        // Create a parent container
        val parent = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        // Inflate the layout with the parent container
        LayoutInflater.from(ctx).inflate(R.layout.screen_stock, parent, false)
    }, update = { view ->
        // Handle the search bar
        val searchIcon = view.findViewById<ImageView>(R.id.searchIcon)
        val searchBarLayout = view.findViewById<LinearLayout>(R.id.searchBarLayout)
        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        val qrCodeButton = view.findViewById<ImageView>(R.id.qrCodeButton)
        val clearSearchButton = view.findViewById<ImageView>(R.id.clearSearchButton)
        val plantList = view.findViewById<RecyclerView>(R.id.plantList)

        // Initialize SearchBar functionality
        Log.d("StockScreen", "Initializing SearchBar")
        SearchBar(
            searchBarLayout = searchBarLayout,
            searchEditText = searchEditText,
            searchIcon = searchIcon,
            qrCodeButton = qrCodeButton,
            clearSearchButton = clearSearchButton,
            onSearch = { query -> viewModel.updateSearchQuery(query) }
        )

        // Handle RecyclerView for displaying plants
        plantList.layoutManager = LinearLayoutManager(view.context)
        val adapter = PlantAdapter(plants)
        plantList.adapter = adapter

        // Ensure adapter is updated with the new plant list
        Log.d("StockScreen", "Updating adapter with plants: ${plants.size}")
        adapter.updatePlants(plants)
        if (plants.isEmpty()) {
            Log.d("StockScreen", "Plants list is empty")
        } else {
            Log.d("StockScreen", "Plants list has ${plants.size} items")
        }

        adapter.setOnItemClickListener { plant ->
            val intent = Intent(view.context, PlantDetailsActivity::class.java).apply {
                putExtra("NameConcat", plant.NameConcat)
                putExtra("StockID", plant.StockID)
                putExtra("StockQty", plant.StockQty)
                putExtra("StockPrice", plant.StockPrice)
                putExtra("Family", plant.Family)
                putExtra("Species", plant.Species)
                putExtra("Subspecies", plant.Subspecies)
                putExtra("PlantDescription", plant.PlantDescription)
                putExtra("PhotoLink1", plant.PhotoLink1)
            }
            plantDetailsLauncher.launch(intent)
        }
    })
}
