package com.example.rrhe

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rrhe.databinding.ActivityEditPlantBinding
import kotlinx.coroutines.launch
import java.util.Date

class EditPlantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPlantBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("plant", Plant::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("plant")
        }

        plant?.let {
            updateUI(it)
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            val updatedPlant = plant?.copy(
                Family = binding.familyEditText.text.toString().ifBlank { "Unknown" },
                Species = binding.speciesEditText.text.toString().ifBlank { "Unknown" },
                Subspecies = binding.subspeciesEditText.text.toString().ifBlank { "" },
                StockQty = binding.stockQtyEditText.text.toString().toInt(),
                StockPrice = binding.stockPriceEditText.text.toString().toDouble(),
                PlantDescription = binding.plantDescriptionEditText.text.toString().ifBlank { "No description" },
                Stamp = Date() // Update the Stamp value to the current date/time
            )

            updatedPlant?.let {
                savePlantLocally(it)
            }
        }

        // Enable OnBackInvokedCallback for API level 33 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Handle the back press
                    finish()
                }
            })
        }
    }

    private fun updateUI(plant: Plant) {
        binding.familyEditText.setText(plant.Family)
        binding.speciesEditText.setText(plant.Species)
        binding.subspeciesEditText.setText(plant.Subspecies)
        binding.stockQtyEditText.setText(plant.StockQty.toString())
        binding.stockPriceEditText.setText(plant.StockPrice.toString())
        binding.plantDescriptionEditText.setText(plant.PlantDescription)
    }

    private fun savePlantLocally(plant: Plant) {
        lifecycleScope.launch {
            PlantRepository.savePlantUpdate(plant)

            // Notify the result intent that the plant was updated
            val resultIntent = Intent().apply {
                putExtra("updated", true)
                putExtra("plant", plant)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
