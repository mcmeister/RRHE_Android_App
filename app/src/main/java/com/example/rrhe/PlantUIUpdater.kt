package com.example.rrhe

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.rrhe.databinding.ActivityEditPlantBinding
import com.example.rrhe.databinding.ActivityNewPlantBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PlantUIUpdater {

    fun updateUI(binding: Any, plant: Plant) {
        updateUICommon(binding, plant) // Delegate to common function
    }

    private fun updateUICommon(binding: Any, plant: Plant) {
        when (binding) {
            is ActivityEditPlantBinding -> {
                // Update UI elements specific to EditPlantActivity
                updateUIElementsForEdit(binding, plant)
            }
            is ActivityNewPlantBinding -> {
                // Update UI elements specific to NewPlantActivity
                updateUIElementsForNew(binding, plant)
            }
        }
    }

    // Update UI elements specifically for EditPlantActivity
    private fun updateUIElementsForEdit(binding: ActivityEditPlantBinding, plant: Plant) {
        binding.familyAutoCompleteTextView.setText(plant.Family)
        binding.speciesAutoCompleteTextView.setText(plant.Species)
        binding.subspeciesAutoCompleteTextView.setText(plant.Subspecies)
        binding.stockQtyEditText.setText(plant.StockQty.toString())
        binding.stockPriceEditText.setText(plant.StockPrice.toString())
        binding.purchasePriceEditText.setText(plant.PurchasePrice?.toString())
        binding.plantDescriptionEditText.setText(plant.PlantDescription)
        binding.thaiNameText.text = plant.ThaiName?.let { decodeUnicode(it) }

        // ID dropdowns
        setIDDropdowns(binding, plant)

        // TableName and other common fields
        binding.traySizeEditText.setText(plant.TraySize)
        binding.gramsEditText.setText(plant.Grams?.toString())
        binding.usdEditText.text = binding.root.context.getString(R.string.usd_edit_text, plant.USD)
        binding.eurEditText.text = binding.root.context.getString(R.string.eur_edit_text, plant.EUR)
        binding.totalValueEditText.text =
            binding.root.context.getString(R.string.total_value_edit_text, plant.TotalValue)

        // Only update photo views and not the date fields
        updatePhotoViews(binding, plant)
    }

    // Update UI elements specifically for NewPlantActivity
    private fun updateUIElementsForNew(binding: ActivityNewPlantBinding, plant: Plant) {
        binding.familyAutoCompleteTextView.setText(plant.Family)
        binding.speciesAutoCompleteTextView.setText(plant.Species)
        binding.subspeciesAutoCompleteTextView.setText(plant.Subspecies)
        binding.stockQtyEditText.setText(plant.StockQty.toString())
        binding.stockPriceEditText.setText(plant.StockPrice.toString())
        binding.purchasePriceEditText.setText(plant.PurchasePrice?.toString())
        binding.plantDescriptionEditText.setText(plant.PlantDescription)
        binding.thaiNameText.text = plant.ThaiName?.let { decodeUnicode(it) }

        // ID dropdowns
        setIDDropdowns(binding, plant)

        // TableName and other common fields
        binding.traySizeEditText.setText(plant.TraySize)
        binding.gramsEditText.setText(plant.Grams?.toString())
        binding.usdEditText.text = binding.root.context.getString(R.string.usd_edit_text, plant.USD)
        binding.eurEditText.text = binding.root.context.getString(R.string.eur_edit_text, plant.EUR)
        binding.totalValueEditText.text =
            binding.root.context.getString(R.string.total_value_edit_text, plant.TotalValue)

        // Update date fields only for NewPlantActivity
        updateDateFields(binding, plant)
        updatePhotoViews(binding, plant)
    }

    // Helper method to handle dropdowns for both Edit and New Plant activities
    private fun setIDDropdowns(binding: Any, plant: Plant) {
        val context = when (binding) {
            is ActivityEditPlantBinding -> binding.root.context
            is ActivityNewPlantBinding -> binding.root.context
            else -> return
        }

        val mIDText = context.getString(
            R.string.plant_id_text_format,
            plant.M_ID,
            plant.NameConcat,
            plant.StockPrice,
            plant.StockQty
        )
        val fIDText = context.getString(
            R.string.plant_id_text_format,
            plant.F_ID,
            plant.NameConcat,
            plant.StockPrice,
            plant.StockQty
        )

        when (binding) {
            is ActivityEditPlantBinding -> {
                binding.mIdAutoCompleteTextView.setText(mIDText, false)
                binding.fIdAutoCompleteTextView.setText(fIDText, false)
            }
            is ActivityNewPlantBinding -> {
                binding.mIdAutoCompleteTextView.setText(mIDText, false)
                binding.fIdAutoCompleteTextView.setText(fIDText, false)
            }
        }
    }

    // Helper method to handle date fields (only for NewPlantActivity)
    private fun updateDateFields(binding: ActivityNewPlantBinding, plant: Plant) {
        val context = binding.root.context

        // Initialize dates if not set
        val today = Calendar.getInstance().time
        val threeWeeksLater = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, 3)
        }.time

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        plant.PlantedStart = plant.PlantedStart ?: dateFormat.format(today)
        plant.PlantedEnd = plant.PlantedEnd ?: dateFormat.format(threeWeeksLater)

        val plantedText = context.getString(
            R.string.date_range_format,
            plant.PlantedStart,
            plant.PlantedEnd
        )

        binding.plantedTextView.text = plantedText
        binding.pollinateDateTextView.text = plant.PollinateDate
        binding.seedsPlantedTextView.text = plant.SeedsPlanted
        binding.seedsHarvestTextView.text = plant.SeedsHarvest
    }

    // Helper method to load and display photos
    private fun updatePhotoViews(binding: Any, plant: Plant) {
        when (binding) {
            is ActivityEditPlantBinding -> {
                loadPhoto(binding.photoEdit1, plant.Photo1)
                loadPhoto(binding.photoEdit2, plant.Photo2)
                loadPhoto(binding.photoEdit3, plant.Photo3)
                loadPhoto(binding.photoEdit4, plant.Photo4)

                binding.photoEdit1.tag = plant.Photo1
                binding.photoEdit2.tag = plant.Photo2
                binding.photoEdit3.tag = plant.Photo3
                binding.photoEdit4.tag = plant.Photo4
            }
            is ActivityNewPlantBinding -> {
                loadPhoto(binding.photoEdit1, plant.Photo1)
                loadPhoto(binding.photoEdit2, plant.Photo2)
                loadPhoto(binding.photoEdit3, plant.Photo3)
                loadPhoto(binding.photoEdit4, plant.Photo4)

                binding.photoEdit1.tag = plant.Photo1
                binding.photoEdit2.tag = plant.Photo2
                binding.photoEdit3.tag = plant.Photo3
                binding.photoEdit4.tag = plant.Photo4
            }
        }
    }

    // Loading photos using Glide
    private fun loadPhoto(imageView: ImageView, photoUrl: String?) {
        if (photoUrl.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.loading_placeholder)  // Default placeholder image
        } else {
            // Add a cache-busting query parameter (timestamp) to ensure the latest image is loaded
            val cacheBustingUrl = "$photoUrl?timestamp=${System.currentTimeMillis()}"

            Glide.with(imageView.context)
                .load(cacheBustingUrl)
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable Glide's disk caching
                    .skipMemoryCache(true) // Skip memory caching
                    .placeholder(R.drawable.loading_placeholder)
                    .error(R.drawable.error_image)
                )
                .into(imageView)
        }
    }
}