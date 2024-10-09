// NewPlantActivity.kt
package com.example.rrhe

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.rrhe.databinding.ActivityNewPlantBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewPlantActivity : AppCompatActivity(), PhotoManager.PlantBinding {

    private lateinit var binding: ActivityNewPlantBinding
    private var motherValue: Int = 0 // Storing mother switch value
    private var websiteValue: Int = 0 // Storing website switch value
    private var tempStockID: Int = 0 // Store generated tempStockID here

    override val photoEdit1: ImageView
        get() = binding.photoEdit1
    override val photoEdit2: ImageView
        get() = binding.photoEdit2
    override val photoEdit3: ImageView
        get() = binding.photoEdit3
    override val photoEdit4: ImageView
        get() = binding.photoEdit4
    override val saveButton: Button
        get() = binding.saveButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Generate the tempStockID once when the activity is created
        lifecycleScope.launch {
            tempStockID = PlantSaveManager.generateTempStockID() // Call the suspend function
            Log.d("NewPlantActivity", "Generated Temp StockID: $tempStockID")
        }

        // Load images or set plus icons for the image views
        loadOrSetPlaceholder(null, binding.photoEdit1)
        loadOrSetPlaceholder(null, binding.photoEdit2)
        loadOrSetPlaceholder(null, binding.photoEdit3)
        loadOrSetPlaceholder(null, binding.photoEdit4)

        setupDropdownAdapters()
        setupTraySizeDropdown() // Added to set up Tray Size dropdown
        setupListeners()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTraySizeDropdown() {
        val predefinedTraySizes = listOf("Tray 6", "Tray 15", "Tray 24")

        val traySizeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, predefinedTraySizes)
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

        // Optionally, set a default value (e.g., the first item)
        // binding.traySizeAutoCompleteTextView.setText(predefinedTraySizes[0], false)
    }

    @Suppress("SameParameterValue")
    private fun loadOrSetPlaceholder(photoUrl: String?, imageView: ImageView) {
        if (photoUrl.isNullOrEmpty()) {
            // Set the plus icon if there's no photo URL
            imageView.setImageResource(R.drawable.ic_add)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        } else {
            // If there's a valid photo URL, load the image
            PhotoManager.loadImageWithRetry(photoUrl, imageView, this)
        }
    }

    private fun recalculateValues() {
        PlantValueCalculator.recalculateValues(
            binding,
            lifecycleScope = lifecycleScope,
            context = this
        )
    }

    private fun handlePlantSave() {
        // InactivityDetector(this).reset()

        lifecycleScope.launch {
            PlantSaveManager.savePlantLocallyAndSync(
                context = this@NewPlantActivity,
                binding = null,
                newBinding = binding,
                currentPlant = null, // No current plant for new plants
                lifecycleScope = lifecycleScope,
                activity = this@NewPlantActivity,
                isEditMode = false,
                tempStockID = tempStockID // Pass the tempStockID here
            )
        }
    }

    private fun setupListeners() {
        // Text change listeners
        PlantListeners.setupTextChangeListeners(
            binding.familyAutoCompleteTextView,
            binding.speciesAutoCompleteTextView,
            binding.subspeciesAutoCompleteTextView,
            binding.stockPriceEditText,
            binding.stockQtyEditText
        ) {
            // InactivityDetector(this).reset()
            recalculateValues()
        }

        // Dropdown listeners
        PlantListeners.setupDropdownListeners(
            binding.familyAutoCompleteTextView,
            binding.speciesAutoCompleteTextView,
            { family ->
                // InactivityDetector(this).reset()
                updateSpeciesDropdown(family)
            },
            { family, species ->
                // InactivityDetector(this).reset()
                updateSubspeciesDropdown(family, species)
            }
        )

        // Save button listener
        binding.saveButton.setOnClickListener {
            // InactivityDetector(this).reset()
            handlePlantSave()
        }

        // Back button listener
        binding.backButton.setOnClickListener {
            // InactivityDetector(this).reset()
            Log.d("NewPlantActivity", "Back button clicked")
            finish()
        }

        // Mother switch listener
        binding.motherSwitch.setOnCheckedChangeListener { _, isChecked ->
            // InactivityDetector(this).reset()
            motherValue = if (isChecked) 1 else 0 // Store the switch value
        }

        // Website switch listener
        binding.websiteSwitch.setOnCheckedChangeListener { _, isChecked ->
            // InactivityDetector(this).reset()
            websiteValue = if (isChecked) 1 else 0 // Store the switch value
        }

        // Date pickers setup using PlantSaveManager
        PlantSaveManager.setupDatePickers(
            PlantSaveManager.BindingWrapper.New(binding),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            Calendar.getInstance(),
            null // No currentPlant for new plants
        ) { datePickerType ->
            when (datePickerType) {
                PlantSaveManager.DatePickerType.POLLINATE_DATE -> {
                    val selectedDate = getSelectedDateOrNull(binding.pollinateDateTextView.text.toString())
                    binding.pollinateDateTextView.text = selectedDate ?: "" // Set to empty if canceled
                }
                PlantSaveManager.DatePickerType.SEEDS_PLANTED -> {
                    val selectedDate = getSelectedDateOrNull(binding.seedsPlantedTextView.text.toString())
                    binding.seedsPlantedTextView.text = selectedDate ?: "" // Set to empty if canceled
                }
                PlantSaveManager.DatePickerType.SEEDS_HARVEST -> {
                    val selectedDate = getSelectedDateOrNull(binding.seedsHarvestTextView.text.toString())
                    binding.seedsHarvestTextView.text = selectedDate ?: "" // Set to empty if canceled
                }
            }
        }

        // Photo click listeners
        PlantListeners.setupPhotoClickListeners(
            listOf(binding.photoEdit1, binding.photoEdit2, binding.photoEdit3, binding.photoEdit4),
            listOf(null, null, null, null), // No photos initially for new plants
            null, // No currentPlant for new plants
            { photoPath, photoIndex ->  // Wrap it in a suspend lambda
                handlePhotoChanged(
                    photoPath = photoPath,
                    photoIndex = photoIndex,
                    currentPlant = null,  // No current plant for new ones
                    tempStockID = tempStockID,  // Use the generated tempStockID
                    isEditMode = false,  // Not in edit mode in NewPlantActivity
                    binding = this@NewPlantActivity as PhotoManager.PlantBinding, // Pass binding to the method
                    activity = this@NewPlantActivity,  // Pass activity to the method
                    scope = lifecycleScope  // Pass coroutine scope to the method
                )
            },
            this::handlePhotoRemoved,
            this
        )

        // Plant status dropdown listener
        binding.plantStatusAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
            val selectedStatus = binding.plantStatusAutoCompleteTextView.text.toString()
            updateParentPlantFieldsVisibility(selectedStatus)
            updatePurchasePriceVisibility(selectedStatus)
            // InactivityDetector(this).reset()
        }
    }

    private fun getSelectedDateOrNull(currentText: String): String? {
        return if (currentText.isEmpty() || currentText == "POLLINATE_DATE" || currentText == "SEEDS_PLANTED" || currentText == "SEEDS_HARVEST") {
            null
        } else {
            currentText
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDropdownAdapters() {
        lifecycleScope.launch {
            // Setup the dropdown adapters (family, status, table name)
            PlantDropdownAdapter.setupFamilyAdapter(applicationContext)
            PlantDropdownAdapter.setupStatusAdapter(applicationContext)
            PlantDropdownAdapter.setupTableNameAdapters(applicationContext)

            // Apply dropdowns to views
            PlantDropdownAdapter.applyFamilyAdapterCommon(
                binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                context = applicationContext,
                currentFamily = ""
            )
            PlantDropdownAdapter.applyStatusAdapter(
                binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                currentStatus = "Purchase"
            )
            PlantDropdownAdapter.applyTableNameAdapters(
                binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                currentTableName = ""
            )

            // Add OnTouchListener to letterSpinner to reset inactivity detector on touch
            binding.letterSpinner.setOnTouchListener { _, _ ->
                // InactivityDetector(this@NewPlantActivity).reset() // Reset inactivity timer
                false // Return false to let the spinner handle the touch event as usual
            }

            // Add OnTouchListener to numberSpinner to reset inactivity detector on touch
            binding.numberSpinner.setOnTouchListener { _, _ ->
                // InactivityDetector(this@NewPlantActivity).reset() // Reset inactivity timer
                false // Return false to let the spinner handle the touch event as usual
            }

            // Update species and subspecies based on family selection
            binding.familyAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
                // InactivityDetector(this@NewPlantActivity).reset() // Reset inactivity timer
                val family = binding.familyAutoCompleteTextView.text.toString()
                updateSpeciesDropdown(family)
            }

            // Initialize parent plant dropdowns (Mother and Father IDs)
            setupParentPlantDropdowns()
        }
    }

    private fun setupParentPlantDropdowns() {
        val family = binding.familyAutoCompleteTextView.text.toString()
        if (family.isNotEmpty()) {
            lifecycleScope.launch {
                val motherPlants = PlantDropdownAdapter.setupMotherPlantIdAdapter(
                    context = applicationContext,
                    family = family,
                    binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope)
                )
                if (motherPlants.isNotEmpty()) {
                    // InactivityDetector(this@NewPlantActivity).reset()
                    PlantDropdownAdapter.applyMotherPlantAdapter(
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                        motherPlants = motherPlants
                    )
                }

                val fatherPlants = PlantDropdownAdapter.setupFatherPlantIdAdapter(
                    context = applicationContext,
                    binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope)
                )
                if (fatherPlants.isNotEmpty()) {
                    // InactivityDetector(this@NewPlantActivity).reset()
                    PlantDropdownAdapter.applyFatherPlantAdapter(
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                        fatherPlants = fatherPlants
                    )
                }
            }
        }
    }

    private fun updateSpeciesDropdown(family: String) {
        lifecycleScope.launch {
            val hasSpecies = PlantDropdownAdapter.updateSpeciesAdapter(applicationContext, family)
            PlantDropdownAdapter.applySpeciesAdapter(
                binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                hasSpecies = hasSpecies,
                currentSpecies = binding.speciesAutoCompleteTextView.text.toString()
            )

            if (hasSpecies) {
                binding.speciesAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
                    val species = binding.speciesAutoCompleteTextView.text.toString()
                    updateSubspeciesDropdown(family, species)
                }
            }
        }
    }

    private fun updateSubspeciesDropdown(family: String, species: String) {
        lifecycleScope.launch {
            val hasSubspecies = PlantDropdownAdapter.updateSubspeciesAdapter(applicationContext, family, species)
            PlantDropdownAdapter.applySubspeciesAdapter(
                binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                hasSubspecies = hasSubspecies
            )
        }
    }

    private fun updateParentPlantFieldsVisibility(plantStatus: String) {
        when (plantStatus) {
            "Propagate" -> {
                binding.mIdLayout.visibility = View.VISIBLE
                binding.fIdLayout.visibility = View.GONE
                lifecycleScope.launch {
                    val motherPlants = PlantDropdownAdapter.setupMotherPlantIdAdapter(
                        context = applicationContext,
                        family = binding.familyAutoCompleteTextView.text.toString(),
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope)
                    )
                    PlantDropdownAdapter.applyMotherPlantAdapter(
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                        motherPlants = motherPlants
                    )
                }
            }
            "Pollinate" -> {
                binding.mIdLayout.visibility = View.VISIBLE
                binding.fIdLayout.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val motherPlants = PlantDropdownAdapter.setupMotherPlantIdAdapter(
                        context = applicationContext,
                        family = binding.familyAutoCompleteTextView.text.toString(),
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope)
                    )
                    PlantDropdownAdapter.applyMotherPlantAdapter(
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                        motherPlants = motherPlants
                    )
                    val fatherPlants = PlantDropdownAdapter.setupFatherPlantIdAdapter(
                        context = applicationContext,
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope)
                    )
                    PlantDropdownAdapter.applyFatherPlantAdapter(
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                        fatherPlants = fatherPlants
                    )
                }
            }
            else -> {
                binding.mIdLayout.visibility = View.GONE
                binding.fIdLayout.visibility = View.GONE
            }
        }
    }

    private fun updatePurchasePriceVisibility(plantStatus: String) {
        if (plantStatus == "Purchase") {
            binding.purchasePriceEditText.visibility = View.VISIBLE
            binding.purchasePriceLabel.visibility = View.VISIBLE
        } else {
            binding.purchasePriceEditText.visibility = View.GONE
            binding.purchasePriceLabel.visibility = View.GONE
        }
    }

    private fun updateImageViewWithGlide(photoPath: String, binding: PhotoManager.PlantBinding, photoIndex: Int, context: Context) {
        Glide.with(context)
            .load(photoPath)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .placeholder(R.drawable.loading_placeholder)
            .error(R.drawable.error_image)
            .into(
                when (photoIndex) {
                    1 -> binding.photoEdit1
                    2 -> binding.photoEdit2
                    3 -> binding.photoEdit3
                    4 -> binding.photoEdit4
                    else -> throw IllegalArgumentException("Invalid photoIndex: $photoIndex")
                }
            )
    }

    private suspend fun handlePhotoChanged(
        photoPath: String,
        photoIndex: Int,
        currentPlant: Plant?,
        tempStockID: Int?, // Pass tempStockID for new plants
        isEditMode: Boolean, // Add isEditMode as a parameter
        binding: PhotoManager.PlantBinding,
        activity: AppCompatActivity,
        scope: CoroutineScope
    ) {
        val uri = Uri.parse(photoPath)

        // Update the ImageView with the photo and cache it
        updateImageViewWithGlide(uri.toString(), binding, photoIndex, activity)

        scope.launch(Dispatchers.IO) {
            // Upload the photo immediately for both Edit and New Plant activities
            PhotoManager.handlePhotoUpload(
                uri,
                activity,
                currentPlant?.StockID ?: tempStockID ?: -1, // Use tempStockID if no current plant
                tempStockID,  // Pass tempStockID for new plants
                isEditMode,   // Check if it's edit mode or not
                photoIndex,
                binding,
                activity,
                scope
            )
        }
    }

    private fun handlePhotoRemoved(photoIndex: Int) {
        PhotoManager.handlePhotoRemoved(photoIndex, null, this, this, lifecycleScope)
    }
}
