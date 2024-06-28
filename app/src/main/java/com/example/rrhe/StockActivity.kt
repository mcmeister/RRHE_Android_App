package com.example.rrhe

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rrhe.databinding.ActivityStockBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockBinding
    private lateinit var searchBar: SearchBar
    private var plants: List<Plant> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.plantList.layoutManager = LinearLayoutManager(this)

        searchBar = SearchBar(
            binding.searchBarLayout,
            binding.searchEditText,
            binding.searchIcon,
            binding.qrCodeButton,
            binding.clearSearchButton
        ) { query -> performSearch(query) }

        fetchRRHE()
    }

    private fun fetchRRHE() {
        Network.apiService.getRRHE().enqueue(object : Callback<List<Plant>> {
            override fun onResponse(call: Call<List<Plant>>, response: Response<List<Plant>>) {
                if (response.isSuccessful) {
                    plants = response.body() ?: emptyList()
                    binding.plantList.adapter = PlantAdapter(plants)
                } else {
                    Log.e("StockActivity", "Failed to fetch data: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<Plant>>, t: Throwable) {
                Log.e("StockActivity", "Failed to fetch data", t)
            }
        })
    }

    private fun performSearch(query: String) {
        val filteredPlants = if (query.isEmpty()) {
            plants
        } else {
            plants.filter {
                it.NameConcat.contains(query, ignoreCase = true) || it.StockID.toString() == query
            }
        }
        binding.plantList.adapter = PlantAdapter(filteredPlants)
    }
}
