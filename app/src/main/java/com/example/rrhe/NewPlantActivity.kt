package com.example.rrhe

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rrhe.databinding.ActivityNewPlantBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewPlantActivity : AppCompatActivity(), PhotoManager.PlantBinding {

    private lateinit var binding: ActivityNewPlantBinding
    private var currentPlant: Plant? = null

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

        currentPlant?.let {
            PlantUIUpdater.updateUI(binding, it) // Utilize PlantUIUpdater to handle UI, including date fields
        }

        setupDropdownAdapters()
        setupListeners()
        setDefaultValues()
    }

    private fun recalculateValues() {
        PlantValueCalculator.recalculateValues(
            binding,
            lifecycleScope = lifecycleScope,
            context = this
        )
    }

    private fun setDefaultValues() {
        // Prepopulate PlantStatus with 'In Stock'
        binding.plantStatusAutoCompleteTextView.setText("In Stock", false)
    }

    private fun handlePlantSave() {
        InactivityDetector(this).reset()
        lifecycleScope.launch {
            // Save plant locally and sync with the main database
            PlantSaveManager.savePlantLocallyAndSync(
                context = this@NewPlantActivity,
                binding = null,
                newBinding = binding,
                currentPlant = currentPlant,
                lifecycleScope = lifecycleScope,
                activity = this@NewPlantActivity,
                isEditMode = false
            )

            // After saving the new plant, navigate back to the Stock Screen
            finish()  // This will close the current activity and go back to the previous one, which is the Stock Screen
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
            InactivityDetector(this).reset()
            recalculateValues()
        }

        // Dropdown listeners
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

        // Save button listener
        binding.saveButton.setOnClickListener {
            InactivityDetector(this).reset()
            handlePlantSave()
        }

        // Back button listener
        binding.backButton.setOnClickListener {
            InactivityDetector(this).reset()
            Log.d("NewPlantActivity", "Back button clicked")
            finish()
        }

        // Mother switch listener
        binding.motherSwitch.setOnCheckedChangeListener { _, isChecked ->
            InactivityDetector(this).reset()
            currentPlant?.Mother = if (isChecked) 1 else 0
        }

        // Website switch listener
        binding.websiteSwitch.setOnCheckedChangeListener { _, isChecked ->
            InactivityDetector(this).reset()
            currentPlant?.Website = if (isChecked) 1 else 0
        }

        // Date pickers setup using PlantSaveManager
        PlantSaveManager.setupDatePickers(
            PlantSaveManager.BindingWrapper.New(binding),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            Calendar.getInstance(),
            currentPlant
        ) { datePickerType ->
            when (datePickerType) {
                PlantSaveManager.DatePickerType.POLLINATE_DATE -> binding.pollinateDateTextView.text = datePickerType.toString()
                PlantSaveManager.DatePickerType.SEEDS_PLANTED -> binding.seedsPlantedTextView.text = datePickerType.toString()
                PlantSaveManager.DatePickerType.SEEDS_HARVEST -> binding.seedsHarvestTextView.text = datePickerType.toString()
            }
        }

        // Photo click listeners
        PlantListeners.setupPhotoClickListeners(
            listOf(binding.photoEdit1, binding.photoEdit2, binding.photoEdit3, binding.photoEdit4),
            listOf(null, null, null, null),
            currentPlant,
            this::handlePhotoChanged,
            this::handlePhotoRemoved,
            this
        )

        // Plant status dropdown listener
        binding.plantStatusAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
            val selectedStatus = binding.plantStatusAutoCompleteTextView.text.toString()
            updateParentPlantFieldsVisibility(selectedStatus)
            updatePurchasePriceVisibility(selectedStatus)
            InactivityDetector(this).reset()
        }
    }

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
                currentStatus = "In Stock"
            )
            PlantDropdownAdapter.applyTableNameAdapters(
                binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                currentTableName = ""
            )

            // Update species and subspecies based on family selection
            binding.familyAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
                val family = binding.familyAutoCompleteTextView.text.toString()
                updateSpeciesDropdown(family)
            }

            // Initialize parent plant dropdowns (Mother and Father IDs)
            setupParentPlantDropdowns()
        }
    }

    private fun updateSpeciesDropdown(family: String) {
        lifecycleScope.launch {
            val hasSpecies = PlantDropdownAdapter.updateSpeciesAdapter(applicationContext, family)
            PlantDropdownAdapter.applySpeciesAdapter(
                binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                hasSpecies = hasSpecies
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
                    InactivityDetector(this@NewPlantActivity).reset()
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
                    InactivityDetector(this@NewPlantActivity).reset()
                    PlantDropdownAdapter.applyFatherPlantAdapter(
                        binding = PlantDropdownAdapter.NewPlantBindingWrapper(binding, lifecycleScope),
                        fatherPlants = fatherPlants
                    )
                }
            }
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

    private fun handlePhotoChanged(photoPath: String, photoIndex: Int) {
        InactivityDetector(this).reset()
        PhotoManager.handlePhotoChanged(photoPath, photoIndex, currentPlant, this, this, lifecycleScope)
    }

    private fun handlePhotoRemoved(photoIndex: Int) {
        InactivityDetector(this).reset()
        PhotoManager.handlePhotoRemoved(photoIndex, currentPlant, this, this, lifecycleScope)
    }
}