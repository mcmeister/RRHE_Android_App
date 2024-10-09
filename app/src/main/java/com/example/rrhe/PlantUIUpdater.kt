package com.example.rrhe

import android.annotation.SuppressLint
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
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
    @SuppressLint("ClickableViewAccessibility")
    private fun updateUIElementsForEdit(binding: ActivityEditPlantBinding, plant: Plant) {
        binding.familyAutoCompleteTextView.setText(plant.Family)
        binding.speciesAutoCompleteTextView.setText(plant.Species)
        binding.subspeciesAutoCompleteTextView.setText(plant.Subspecies)
        binding.stockQtyEditText.setText(plant.StockQty.toString())
        binding.stockPriceEditText.setText(plant.StockPrice.toString())
        binding.purchasePriceEditText.setText(plant.PurchasePrice?.toString())
        binding.plantDescriptionEditText.setText(plant.PlantDescription)
        binding.thaiNameText.text = plant.ThaiName?.let { decodeUnicode(it) }

        // Setup Tray Size dropdown
        val predefinedTraySizes = mutableListOf("Tray 6", "Tray 15", "Tray 24")

        // Add existing Tray Size if not in predefined list
        plant.TraySize?.let { traySize ->
            if (!predefinedTraySizes.contains(traySize)) {
                predefinedTraySizes.add(traySize)
            }
        }

        val traySizeAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_dropdown_item_1line, predefinedTraySizes)
        binding.traySizeAutoCompleteTextView.setAdapter(traySizeAdapter)

        // Prevent typing
        binding.traySizeAutoCompleteTextView.inputType = InputType.TYPE_NULL
        binding.traySizeAutoCompleteTextView.keyListener = null

        // Show dropdown on click
        binding.traySizeAutoCompleteTextView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.traySizeAutoCompleteTextView.showDropDown()
                binding.traySizeAutoCompleteTextView.requestFocus()
            }
            false
        }

        // Set the existing value
        binding.traySizeAutoCompleteTextView.setText(plant.TraySize ?: "", false)

        // Handle TableName substrings
        if (!plant.TableName.isNullOrEmpty()) {
            updateTableNameSpinners(binding, plant.TableName!!)
        } else {
            // If TableName is null or empty, clear the spinners
            binding.letterSpinner.setSelection(0)
            binding.numberSpinner.setSelection(0)
        }

        // Continue with other updates
        setIDDropdowns(binding, plant)
        updatePhotoViews(binding, plant)
    }

    private fun updateTableNameSpinners(binding: ActivityEditPlantBinding, tableName: String) {
        if (tableName.length >= 2) {
            val letter = tableName.substring(0, 1)
            val numberString = tableName.substring(1)
            val number = numberString.toIntOrNull()

            if (number != null) {
                // Set the letter spinner
                val letterPosition = PlantDropdownAdapter.letterAdapter?.getPosition(letter) ?: 0
                binding.letterSpinner.setSelection(letterPosition)

                // Create a PlantBindingWrapper
                val bindingWrapper = PlantDropdownAdapter.EditPlantBindingWrapper(binding)

                // Update the number spinner based on the letter and desired number
                PlantDropdownAdapter.updateNumberSpinner(bindingWrapper, letter, number)

                // Removed the redundant post block
            } else {
                Log.e("PlantUIUpdater", "Invalid number in TableName: $tableName")
                // Optionally, reset spinners to default selections
                binding.letterSpinner.setSelection(0)
                val bindingWrapper = PlantDropdownAdapter.EditPlantBindingWrapper(binding)
                PlantDropdownAdapter.updateNumberSpinner(bindingWrapper, "", null)
            }
        } else {
            Log.e("PlantUIUpdater", "Invalid TableName format: $tableName")
            // Optionally, reset spinners to default selections
            binding.letterSpinner.setSelection(0)
            val bindingWrapper = PlantDropdownAdapter.EditPlantBindingWrapper(binding)
            PlantDropdownAdapter.updateNumberSpinner(bindingWrapper, "", null)
        }
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
        binding.traySizeAutoCompleteTextView.setText(plant.TraySize)
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

    // Loading photos using Glide, but only if the photo URL is not null or empty
    private fun loadPhoto(imageView: ImageView, photoUrl: String?) {
        val trimmedPhotoUrl = photoUrl?.trim()
        if (!trimmedPhotoUrl.isNullOrEmpty() && !trimmedPhotoUrl.equals("null", ignoreCase = true)) {
            // Add a cache-busting query parameter (timestamp) to ensure the latest image is loaded
            val cacheBustingUrl = "$trimmedPhotoUrl?timestamp=${System.currentTimeMillis()}"

            Glide.with(imageView.context)
                .load(cacheBustingUrl)
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable Glide's disk caching
                    .skipMemoryCache(true) // Skip memory caching
                    .placeholder(R.drawable.loading_placeholder) // Show loading placeholder while image loads
                    .error(R.drawable.error_image) // Show error image if loading fails
                )
                .into(imageView)
        } else {
            // Log the invalid URL
            Log.d("PlantUIUpdater", "Photo URL is null or invalid, setting plus icon as placeholder.")
            // Set the plus icon if the photo URL is null, empty, or invalid
            imageView.setImageResource(R.drawable.ic_add)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }
}