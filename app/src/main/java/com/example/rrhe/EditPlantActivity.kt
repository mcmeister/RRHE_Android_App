package com.example.rrhe

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.rrhe.databinding.ActivityEditPlantBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

interface PlantBindingActivity {
    val familyAutoCompleteTextView: AutoCompleteTextView
    val plantStatusAutoCompleteTextView: AutoCompleteTextView
    val letterSpinner: Spinner
    val numberSpinner: Spinner
    val mIdAutoCompleteTextView: AutoCompleteTextView
    val fIdAutoCompleteTextView: AutoCompleteTextView

    val photoEdit1: ImageView
    val photoEdit2: ImageView
    val photoEdit3: ImageView
    val photoEdit4: ImageView
    val saveButton: Button
}

class EditPlantActivity : AppCompatActivity(), PhotoManager.PlantBinding, PlantBindingActivity {

    private lateinit var binding: ActivityEditPlantBinding
    private lateinit var backPressedCallback: OnBackPressedCallback
    private var currentPlant: Plant? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val cameraRequestCode = 101

    private var datePickerType: PlantSaveManager.DatePickerType? = null

    private var isPlantSaved = false

    private var imagesLoaded = false

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

    override val familyAutoCompleteTextView: AutoCompleteTextView
        get() = binding.familyAutoCompleteTextView
    override val plantStatusAutoCompleteTextView: AutoCompleteTextView
        get() = binding.plantStatusAutoCompleteTextView
    override val letterSpinner: Spinner
        get() = binding.letterSpinner
    override val numberSpinner: Spinner
        get() = binding.numberSpinner
    override val mIdAutoCompleteTextView: AutoCompleteTextView
        get() = binding.mIdAutoCompleteTextView
    override val fIdAutoCompleteTextView: AutoCompleteTextView
        get() = binding.fIdAutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("EditPlantActivity", "onCreate called")
        super.onCreate(savedInstanceState)

        binding = ActivityEditPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adjustLabelColors()

        currentPlant = getPlantFromIntent()

        // Log plant details for debugging
        Log.d("EditPlantActivity", "Current plant: ${currentPlant?.TableName ?: "No plant data"}")

        // Setup dropdown adapters (do this once)
        setupDropdownAdapters(currentPlant)

        currentPlant?.let {
            // Update the UI based on currentPlant (without re-setting the dropdowns)
            if (binding.letterSpinner.selectedItem == null || binding.numberSpinner.selectedItem == null) {
                // Only update spinners once if not set already
                updateTableNameDropdown(it.TableName ?: "")
            }
            // This method handles other UI elements unrelated to spinners
            PlantUIUpdater.updateUI(binding, it)
        }

        setupListeners()
        setupBackButtonCallback()
    }

    private fun adjustLabelColors() {
        val textColor = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            ContextCompat.getColor(this, R.color.dark_mode_text_color) // Define a custom color resource in your colors.xml
        } else {
            ContextCompat.getColor(this, R.color.default_label_color)
        }

        binding.familyAutoCompleteTextView.setHintTextColor(textColor)
        binding.speciesAutoCompleteTextView.setHintTextColor(textColor)
        binding.subspeciesAutoCompleteTextView.setHintTextColor(textColor)
        binding.mIdAutoCompleteTextView.setHintTextColor(textColor)
        binding.fIdAutoCompleteTextView.setHintTextColor(textColor)
    }

    override fun onResume() {
        Log.d("EditPlantActivity", "onResume called")
        super.onResume()

        currentPlant?.let {
            // Avoid resetting images if photo is uploading
            if (!PlantSaveManager.isPhotoUploading) {
                Log.d("onResume", "Current plant in onResume: ${it.TableName}")

                // Only call this once, as it may reset spinner selections
                if (binding.numberSpinner.selectedItem == null || binding.letterSpinner.selectedItem == null) {
                    setupDropdownAdapters(it)
                }

                // Ensure we don't re-set the dropdown values here unless needed
                if (binding.letterSpinner.selectedItem == null || binding.numberSpinner.selectedItem == null) {
                    updateTableNameDropdown(it.TableName ?: "")
                }

                // Delay the log of spinner selections until the layout is fully ready
                binding.letterSpinner.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            Log.d("onResume", "Letter spinner selection after resume: ${binding.letterSpinner.selectedItem}")
                            binding.letterSpinner.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                )

                binding.numberSpinner.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            Log.d("onResume", "Number spinner selection after resume: ${binding.numberSpinner.selectedItem}")
                            binding.numberSpinner.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                )
            }
            updateUI(it)
        }

        InactivityDetector(this).reset()
    }

    override fun onPause() {
        super.onPause()
        InactivityDetector(this).stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("EditPlantActivity", "onDestroy called")
        if (::backPressedCallback.isInitialized) {
            backPressedCallback.remove()
        }
        PhotoManager.clearGlideCache(applicationContext)

        // Only delete temp files if the plant was saved
        if (isPlantSaved) {
            PhotoManager.deleteTempFiles()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        InactivityDetector(this).reset()
        return super.onTouchEvent(event)
    }

    private fun getPlantFromIntent(): Plant? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("plant", Plant::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("plant")
        }
    }

    private fun updateParentPlantFieldsVisibility(plantStatus: String) {
        val bindingWrapper = PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope)

        when (plantStatus) {
            "Propagate" -> {
                binding.mIdLayout.visibility = View.VISIBLE
                binding.fIdLayout.visibility = View.GONE
                lifecycleScope.launch {
                    PlantDropdownAdapter.setupMotherPlantIdAdapter(
                        applicationContext,
                        binding.familyAutoCompleteTextView.text.toString(),
                        bindingWrapper
                    )
                }
            }
            "Pollinate" -> {
                binding.mIdLayout.visibility = View.VISIBLE
                binding.fIdLayout.visibility = View.VISIBLE
                lifecycleScope.launch {
                    PlantDropdownAdapter.setupMotherPlantIdAdapter(
                        applicationContext,
                        binding.familyAutoCompleteTextView.text.toString(),
                        bindingWrapper
                    )
                    PlantDropdownAdapter.setupFatherPlantIdAdapter(
                        applicationContext,
                        bindingWrapper
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

    private fun updateUI(plant: Plant) {
        PlantUIUpdater.updateUI(binding, plant)
        binding.motherSwitch.isChecked = plant.Mother == 1
        binding.websiteSwitch.isChecked = plant.Website == 1
        PlantSaveManager.updateUI(binding, plant)

        // Populate Species, StockID, and StatusNote
        binding.speciesAutoCompleteTextView.setText(plant.Species ?: "", false)
        binding.stockIdText.text = plant.StockID?.toString() ?: ""
        binding.statusNoteEditText.setText(plant.StatusNote ?: "")

        plant.PlantStatus?.let { updateParentPlantFieldsVisibility(it) }
        plant.PlantStatus?.let { updatePurchasePriceVisibility(it) }

        if (!imagesLoaded) {
            loadOrSetPlaceholder(plant.Photo1, binding.photoEdit1)
            loadOrSetPlaceholder(plant.Photo2, binding.photoEdit2)
            loadOrSetPlaceholder(plant.Photo3, binding.photoEdit3)
            loadOrSetPlaceholder(plant.Photo4, binding.photoEdit4)
            imagesLoaded = true
        }
    }

    private fun loadOrSetPlaceholder(photoUrl: String?, imageView: ImageView) {
        if (imageView.drawable == null || photoUrl.isNullOrEmpty()) {
            // Only set the plus icon if there's no photo URL and if the ImageView is empty
            imageView.setImageResource(R.drawable.ic_add)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        } else {
            // If there's a valid photo URL and the ImageView is not empty, load the image
            PhotoManager.loadImageWithRetry(photoUrl, imageView, this)
        }
    }

    private fun setupListeners() {
        PlantListeners.setupTextChangeListeners(
            binding.familyAutoCompleteTextView,
            binding.speciesAutoCompleteTextView,
            binding.subspeciesAutoCompleteTextView,
            binding.stockPriceEditText,
            binding.stockQtyEditText
        ) {
            InactivityDetector(this).reset()
            recalculateValues()
        }
        PlantListeners.setupDropdownListeners(
            binding.familyAutoCompleteTextView,
            binding.speciesAutoCompleteTextView,
            { family ->
                InactivityDetector(this).reset()
                updateSpeciesDropdown(family)
            },
            { family, species ->
                InactivityDetector(this).reset()
                updateSubspeciesDropdown(family, species)
            }
        )
        PlantListeners.setupSaveButtonListener(binding.saveButton, currentPlant, this::savePlantLocallyAndSync)

        binding.backButton.setOnClickListener {
            InactivityDetector(this).reset()
            Log.d("EditPlantActivity", "Back button clicked")
            finish()
        }

        binding.motherSwitch.setOnCheckedChangeListener { _, isChecked ->
            InactivityDetector(this).reset()
            currentPlant?.Mother = if (isChecked) 1 else 0
        }

        binding.websiteSwitch.setOnCheckedChangeListener { _, isChecked ->
            InactivityDetector(this).reset()
            currentPlant?.Website = if (isChecked) 1 else 0
        }

        setupDatePickers()

        PlantListeners.setupPhotoClickListeners(
            listOf(binding.photoEdit1, binding.photoEdit2, binding.photoEdit3, binding.photoEdit4),
            listOf(currentPlant?.Photo1, currentPlant?.Photo2, currentPlant?.Photo3, currentPlant?.Photo4),
            currentPlant,
            this::handlePhotoChanged,
            this::handlePhotoRemoved,
            this
        )

        binding.plantStatusAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
            val selectedStatus = binding.plantStatusAutoCompleteTextView.text.toString()
            updateParentPlantFieldsVisibility(selectedStatus)
            updatePurchasePriceVisibility(selectedStatus)
            InactivityDetector(this).reset()
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        PlantSaveManager.setupDatePickers(
            PlantSaveManager.BindingWrapper.Edit(binding),
            dateFormat,
            calendar,
            currentPlant
        ) { newDatePickerType ->
            datePickerType = newDatePickerType // Use the unified enum
        }
    }

    private fun setupBackButtonCallback() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("EditPlantActivity", "System back button pressed")
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun recalculateValues() {
        PlantValueCalculator.recalculateValues(
            binding = binding,
            lifecycleScope = lifecycleScope,
            context = this
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDropdownAdapters(plant: Plant?) {
        Log.d("EditPlantActivity", "Setting up dropdown adapters for plant: $plant")

        lifecycleScope.launch {
            // Setup the adapters for Family, Status, and Table Name
            PlantDropdownAdapter.setupFamilyAdapter(applicationContext)
            PlantDropdownAdapter.setupStatusAdapter(applicationContext)
            PlantDropdownAdapter.setupTableNameAdapters(applicationContext)

            // Apply the family adapter to the Family dropdown
            PlantDropdownAdapter.applyFamilyAdapterCommon(
                PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                applicationContext,
                plant?.Family ?: ""
            )

            // Apply the status adapter to the Status dropdown
            PlantDropdownAdapter.applyStatusAdapter(
                PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                plant?.PlantStatus ?: ""
            )

            // Ensure we update both letter and number spinners properly
            plant?.TableName?.let {
                // Apply adapters first to ensure we can set selections
                Log.d("setupDropdownAdapters", "Applying TableName adapters for TableName: $it")
                PlantDropdownAdapter.applyTableNameAdapters(
                    PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                    it
                )

                // Now update spinners after adapters are applied
                updateTableNameDropdown(it)
            }

            // Add OnTouchListener to reset inactivity timer on user interaction
            binding.letterSpinner.setOnTouchListener { _, _ ->
                InactivityDetector(this@EditPlantActivity).reset()
                false
            }

            binding.numberSpinner.setOnTouchListener { _, _ ->
                InactivityDetector(this@EditPlantActivity).reset()
                false
            }

            // Update species dropdown if Family is set
            plant?.Family?.let { family ->
                if (family.isNotEmpty()) {
                    updateSpeciesDropdown(family, plant.Species ?: "")
                }
            }

            // Setup parent plant dropdowns if necessary
            setupParentPlantDropdowns(plant)
        }
    }

    private fun updateTableNameDropdown(tableName: String) {
        Log.d("EditPlantActivity", "Updating TableName dropdowns for: $tableName")

        if (tableName.length > 1) {
            val letter = tableName.substring(0, 1) // Extract the letter
            val number = tableName.substring(1).toIntOrNull() // Extract the number

            if (number != null) {
                Log.d("updateTableNameDropdown", "Parsed TableName: Letter = $letter, Number = $number")

                // Set the letter spinner
                binding.letterSpinner.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            Log.d("updateTableNameDropdown", "Setting letter spinner to: $letter")
                            binding.letterSpinner.setSelection(
                                PlantDropdownAdapter.letterAdapter?.getPosition(letter) ?: 0
                            )
                            binding.letterSpinner.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                )

                // Set the number spinner (zero-based index)
                binding.numberSpinner.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            Log.d("updateTableNameDropdown", "Setting number spinner to: $number")
                            binding.numberSpinner.setSelection(number - 1)
                            binding.numberSpinner.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                )
            } else {
                Log.w("updateTableNameDropdown", "Invalid number in TableName: $tableName")
            }
        } else {
            Log.w("updateTableNameDropdown", "Invalid TableName format: $tableName")
        }
    }

    private fun setupParentPlantDropdowns(plant: Plant?) {
        plant?.Family?.let { family ->
            lifecycleScope.launch {
                val motherPlants = PlantDropdownAdapter.setupMotherPlantIdAdapter(
                    applicationContext,
                    family,
                    PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope)
                )
                if (motherPlants.isNotEmpty()) {
                    PlantDropdownAdapter.applyMotherPlantAdapter(
                        PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                        motherPlants
                    )
                }

                val fatherPlants = PlantDropdownAdapter.setupFatherPlantIdAdapter(
                    applicationContext,
                    PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope)
                )
                if (fatherPlants.isNotEmpty()) {
                    PlantDropdownAdapter.applyFatherPlantAdapter(
                        PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                        fatherPlants
                    )
                }
            }
        }
    }

    private fun updateSpeciesDropdown(family: String, currentSpecies: String = "") {
        Log.d("EditPlantActivity", "Updating species dropdown for family: $family")
        val bindingWrapper = PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope)

        lifecycleScope.launch {
            val hasSpecies = PlantDropdownAdapter.updateSpeciesAdapter(applicationContext, family)
            PlantDropdownAdapter.applySpeciesAdapter(bindingWrapper, hasSpecies)

            if (currentSpecies.isNotEmpty() && hasSpecies) {
                updateSubspeciesDropdown(family, currentSpecies, binding.subspeciesAutoCompleteTextView.text.toString())
            } else {
                binding.subspeciesAutoCompleteTextView.text.clear()
            }
        }
    }

    private fun updateSubspeciesDropdown(family: String, species: String, currentSubspecies: String = "") {
        Log.d("EditPlantActivity", "Updating subspecies dropdown for family: $family and species: $species")
        val bindingWrapper = PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope)

        lifecycleScope.launch {
            val hasSubspecies = PlantDropdownAdapter.updateSubspeciesAdapter(applicationContext, family, species)
            PlantDropdownAdapter.applySubspeciesAdapter(bindingWrapper, hasSubspecies)

            if (hasSubspecies) {
                if (currentSubspecies.isNotEmpty()) {
                    binding.subspeciesAutoCompleteTextView.setText(currentSubspecies, false)
                }
            } else {
                binding.subspeciesAutoCompleteTextView.text.clear()
            }
        }
    }

    private fun savePlantLocallyAndSync(plant: Plant) {
        InactivityDetector(this).reset()
        if (PlantSaveManager.isPhotoUploading) {
            showToast(this, "Please wait until the photo is uploaded")
            return
        }

        val job = SupervisorJob()

        lifecycleScope.launch(job + Dispatchers.IO) {
            try {
                Log.d("EditPlantActivity", "Saving plant locally and syncing")
                PlantSaveManager.savePlantLocallyAndSync(
                    context = applicationContext,
                    binding = binding,
                    currentPlant = plant,
                    lifecycleScope = lifecycleScope,
                    activity = this@EditPlantActivity,
                    isEditMode = true // Assuming this is for editing
                )

                withContext(Dispatchers.Main) {
                    plant.StockID?.let {
                        // Clear cache only after the plant is saved
                        PhotoManager.clearGlideCache(applicationContext)
                    }
                    // Mark the plant as saved after successfully saving
                    isPlantSaved = true
                }

            } catch (e: Exception) {
                Log.e("EditPlantActivity", "Error saving plant locally and syncing: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast(this@EditPlantActivity, "Error saving plant: ${e.message}")
                }
            } finally {
                // Clear temp files after saving
                PhotoManager.deleteTempFiles()
            }
        }.invokeOnCompletion { throwable ->
            throwable?.let {
                Log.e("EditPlantActivity", "Sync job was cancelled: ${it.message}")
                lifecycleScope.launch(Dispatchers.Main) {
                    showToast(this@EditPlantActivity, "Sync job was cancelled")
                }
            }
        }
    }

    private fun handlePhotoChanged(photoPath: String, photoIndex: Int) {
        InactivityDetector(this).reset()
        imagesLoaded = false

        // Call PhotoManager to handle the photo change and upload
        PhotoManager.handlePhotoChanged(
            photoPath = photoPath,
            photoIndex = photoIndex,
            currentPlant = currentPlant,  // Pass the current plant
            tempStockID = null,  // No tempStockID for EditPlantActivity
            isEditMode = true,  // Always edit mode in EditPlantActivity
            binding = this as PhotoManager.PlantBinding,  // Assuming this implements PlantBinding
            activity = this,
            scope = lifecycleScope
        )
    }

    private fun handlePhotoRemoved(photoIndex: Int) {
        InactivityDetector(this).reset()
        PhotoManager.handlePhotoRemoved(photoIndex, currentPlant, binding, this, lifecycleScope) // Pass the correct binding
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PhotoManager.onRequestPermissionsResult(requestCode, grantResults, cameraRequestCode, this, this)
    }
}