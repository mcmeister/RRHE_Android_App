package com.example.rrhe

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.rrhe.databinding.ActivityPlantDetailsBinding
import java.util.Date

class PlantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantDetailsBinding
    private var stockID: Int = 0
    private lateinit var editPlantActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editPlantActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updatedPlant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra("plant", Plant::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra("plant")
                }
                updatedPlant?.let { updatePlantDetails(it) }
            }
        }

        val nameConcat = intent.getStringExtra("NameConcat") ?: ""
        stockID = intent.getIntExtra("StockID", 0)
        val stockQty = intent.getIntExtra("StockQty", 0)
        val stockPrice = intent.getDoubleExtra("StockPrice", 0.0)
        val photoLink1 = intent.getStringExtra("PhotoLink1") ?: ""
        val family = intent.getStringExtra("Family") ?: ""
        val species = intent.getStringExtra("Species") ?: ""
        val subspecies = intent.getStringExtra("Subspecies") ?: ""
        val plantDescription = intent.getStringExtra("PlantDescription") ?: ""
        val stamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("Stamp", Date::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("Stamp") as? Date
        } ?: Date() // Provide a default value if null

        val plant = Plant(
            StockID = stockID,
            NameConcat = nameConcat,
            Family = family,
            Species = species,
            Subspecies = subspecies,
            StockQty = stockQty,
            StockPrice = stockPrice,
            PlantDescription = plantDescription,
            PhotoLink1 = photoLink1,
            Stamp = stamp // Set Stamp value
        )

        updatePlantDetails(plant)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editButton.setOnClickListener {
            val intent = Intent(this, EditPlantActivity::class.java).apply {
                putExtra("plant", plant)
            }
            editPlantActivityResultLauncher.launch(intent)
        }

        // Observe changes in the plant list
        PlantRepository.plants.observe(this) { plants ->
            val updatedPlant = plants.find { it.StockID == stockID }
            updatedPlant?.let { updatePlantDetails(it) }
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

    private fun updatePlantDetails(plant: Plant) {
        binding.plantName.text = plant.NameConcat
        binding.plantStockId.text = getString(R.string.stock_id_text, plant.StockID)
        binding.plantStockQty.text = getString(R.string.stock_text, plant.StockQty)
        binding.plantStockPrice.text = getString(R.string.stock_price_text, plant.StockPrice)
        loadImage(plant.PhotoLink1)
    }

    private fun loadImage(photoLink: String?) {
        if (!photoLink.isNullOrEmpty() && (photoLink.startsWith("http://") || photoLink.startsWith("https://"))) {
            Glide.with(this)
                .load(photoLink)
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.loading_placeholder) // Placeholder image
                    .error(R.drawable.error_image) // Error image
                )
                .into(binding.plantImage)
        } else {
            // Show error image if the link is empty, null, or invalid
            Glide.with(this)
                .load(R.drawable.error_image) // Error image
                .into(binding.plantImage)
        }
    }
}
