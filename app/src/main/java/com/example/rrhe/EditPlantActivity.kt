package com.example.rrhe

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rrhe.databinding.ActivityEditPlantBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            binding.familyEditText.setText(it.Family)
            binding.speciesEditText.setText(it.Species)
            binding.subspeciesEditText.setText(it.Subspecies)
            binding.stockQtyEditText.setText(it.StockQty.toString())
            binding.stockPriceEditText.setText(it.StockPrice.toString())
            binding.plantDescriptionEditText.setText(it.PlantDescription)
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            val updatedPlant = PlantUpdateRequest(
                StockID = plant?.StockID ?: 0,
                Family = binding.familyEditText.text.toString(),
                Species = binding.speciesEditText.text.toString(),
                Subspecies = binding.subspeciesEditText.text.toString(),
                StockQty = binding.stockQtyEditText.text.toString().toInt(),
                StockPrice = binding.stockPriceEditText.text.toString().toDouble(),
                PlantDescription = binding.plantDescriptionEditText.text.toString()
            )
            updatePlant(updatedPlant)
        }
    }

    private fun updatePlant(plant: PlantUpdateRequest) {
        Network.apiService.updatePlant(plant).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditPlantActivity, "Plant updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditPlantActivity, "Failed to update plant", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@EditPlantActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
