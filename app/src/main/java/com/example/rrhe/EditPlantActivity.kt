package com.example.rrhe

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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
        super.onCreate(savedInstanceState)
        binding = ActivityEditPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPlant = getPlantFromIntent()

        currentPlant?.let {
            PlantUIUpdater.updateUI(binding, it) // Utilize PlantUIUpdater to handle UI, including date fields
        }

        setupDropdownAdapters(currentPlant)
        setupListeners()
        setupBackButtonCallback()
    }

    override fun onResume() {
        super.onResume()
        currentPlant?.let {
            updateUI(it)
            setupDropdownAdapters(it)
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
        PhotoManager.deleteTempFiles()
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

    fun updateUI(plant: Plant) {
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

        // Load images or set plus icon if photo is null or empty
        loadOrSetPlaceholder(plant.Photo1, binding.photoEdit1)
        loadOrSetPlaceholder(plant.Photo2, binding.photoEdit2)
        loadOrSetPlaceholder(plant.Photo3, binding.photoEdit3)
        loadOrSetPlaceholder(plant.Photo4, binding.photoEdit4)
    }

    private fun loadOrSetPlaceholder(photoUrl: String?, imageView: ImageView) {
        if (photoUrl.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_add) // Use your plus icon resource here
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        } else {
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

    private fun setupDropdownAdapters(plant: Plant?) {
        Log.d("EditPlantActivity", "Setting up dropdown adapters")
        lifecycleScope.launch {
            // Setup the adapters
            PlantDropdownAdapter.setupFamilyAdapter(applicationContext)
            PlantDropdownAdapter.setupStatusAdapter(applicationContext)
            PlantDropdownAdapter.setupTableNameAdapters(applicationContext)

            // Apply the adapters to the views
            PlantDropdownAdapter.applyFamilyAdapterCommon(
                PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                applicationContext,
                plant?.Family ?: ""
            )
            PlantDropdownAdapter.applyStatusAdapter(
                PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                plant?.PlantStatus ?: ""
            )
            PlantDropdownAdapter.applyTableNameAdapters(
                PlantDropdownAdapter.EditPlantBindingWrapper(binding, lifecycleScope),
                plant?.TableName ?: ""
            )

            // Add listeners for letter and number spinners to reset inactivity detector
            binding.letterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    InactivityDetector(this@EditPlantActivity).reset()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }

            binding.numberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    InactivityDetector(this@EditPlantActivity).reset()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }

            // Update species dropdown if family is set
            plant?.Family?.let { family ->
                if (family.isNotEmpty()) {
                    updateSpeciesDropdown(family, plant.Species ?: "")
                }
            }

            // Setup parent plant dropdowns
            setupParentPlantDropdowns(plant)
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
                    isEditMode = true // Assuming this is for editing, set this based on your use case
                )

                withContext(Dispatchers.Main) {
                    plant.StockID?.let {
                        PhotoManager.refreshPhotos(it, this@EditPlantActivity, this@EditPlantActivity, lifecycleScope)
                    }
                }

            } catch (e: Exception) {
                Log.e("EditPlantActivity", "Error saving plant locally and syncing: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast(this@EditPlantActivity, "Error saving plant: ${e.message}")
                }
            } finally {
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
        PhotoManager.handlePhotoChanged(photoPath, photoIndex, currentPlant, this, this, lifecycleScope)
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
