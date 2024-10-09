package com.example.rrhe

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.rrhe.databinding.ActivityEditPlantBinding
import com.example.rrhe.databinding.ActivityNewPlantBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PlantDropdownAdapter {

    private var familyAdapter: ArrayAdapter<String>? = null
    private var speciesAdapter: ArrayAdapter<String>? = null
    private var subspeciesAdapter: ArrayAdapter<String>? = null
    private var statusAdapter: ArrayAdapter<String>? = null
    var letterAdapter: ArrayAdapter<String>? = null
    private var numberAdapter: ArrayAdapter<String>? = null
    private var motherPlantAdapter: ArrayAdapter<String>? = null
    private var fatherPlantAdapter: ArrayAdapter<String>? = null

    suspend fun setupFamilyAdapter(context: Context) {
        Log.d("PlantDropdownAdapter", "Fetching unique families for dropdown setup")
        val families = PlantRepository.getUniqueFamilies(context)
        Log.d("PlantDropdownAdapter", "Setting up Family dropdown with ${families.size} items")

        familyAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            families
        )
    }

    fun setupStatusAdapter(context: Context) {
        Log.d("PlantDropdownAdapter", "Setting up Status dropdown")
        val statuses = context.resources.getStringArray(R.array.statuses)

        statusAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            statuses
        )
    }

    fun setupTableNameAdapters(context: Context) {
        val letters = context.resources.getStringArray(R.array.letter_array).toMutableList()
        letters.add(0, "") // Add empty option at the beginning

        letterAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            letters
        )

        // Similar adjustment for numberAdapter
        val numbers = context.resources.getStringArray(R.array.number_array).toMutableList()
        numbers.add(0, "") // Add empty option at the beginning

        numberAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            numbers
        )
    }

    fun applyFamilyAdapterCommon(binding: PlantBindingWrapper, context: Context, currentFamily: String) {
        binding.runOnUiThread {
            binding.familyAutoCompleteTextView.setAdapter(familyAdapter)
            binding.familyAutoCompleteTextView.setText(currentFamily, false)
            binding.familyAutoCompleteTextView.threshold = 0
            binding.familyAutoCompleteTextView.setOnClickListener {
                binding.familyAutoCompleteTextView.showDropDown()
            }

            binding.familyAutoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val familyName = binding.familyAutoCompleteTextView.text.toString()
                    binding.lifecycleScope.launch {
                        handleFamilySelection(context, familyName, binding)
                    }
                }
            }

            binding.familyAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
                val familyName = binding.familyAutoCompleteTextView.text.toString()
                binding.lifecycleScope.launch {
                    handleFamilySelection(context, familyName, binding)
                }
            }
        }
    }

    fun applyStatusAdapter(binding: PlantBindingWrapper, currentStatus: String) {
        binding.runOnUiThread {
            Log.d("PlantDropdownAdapter", "Applying status adapter")
            binding.plantStatusAutoCompleteTextView.setAdapter(statusAdapter)
            binding.plantStatusAutoCompleteTextView.setText(currentStatus, false)
            binding.plantStatusAutoCompleteTextView.threshold = 0
            binding.plantStatusAutoCompleteTextView.setOnClickListener {
                binding.plantStatusAutoCompleteTextView.showDropDown()
            }
        }
    }

    fun applyTableNameAdapters(binding: PlantBindingWrapper, currentTableName: String) {
        binding.runOnUiThread {
            Log.d("PlantDropdownAdapter", "Applying TableName: '$currentTableName'")

            // Apply the letter dropdown
            binding.letterSpinner.adapter = letterAdapter

            // Listener for letter spinner to update the number spinner
            binding.letterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLetter = parent.getItemAtPosition(position) as String
                    Log.d("PlantDropdownAdapter", "Letter spinner selected: '$selectedLetter' at position: $position")

                    // Only apply this logic if currentTableName is not empty (for Edit Plant)
                    val desiredNumber = if (currentTableName.isNotEmpty() && currentTableName.length >= 2 && currentTableName.startsWith(selectedLetter)) {
                        currentTableName.substring(1).toIntOrNull()
                    } else {
                        null
                    }

                    Log.d("PlantDropdownAdapter", "Desired number for selected letter '$selectedLetter': $desiredNumber")

                    // Update the number spinner with the selected letter and desired number
                    updateNumberSpinner(binding, selectedLetter, desiredNumber)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }

            // Apply TableName parsing only if it's not empty (to avoid affecting Edit Plant)
            if (currentTableName.isNotEmpty() && currentTableName.length >= 2) {
                val letter = currentTableName.substring(0, 1)
                val numberString = currentTableName.substring(1)
                val number = numberString.toIntOrNull()

                Log.d("PlantDropdownAdapter", "Parsed TableName: Letter = '$letter', Number = $number")

                if (number != null) {
                    // Set the letter spinner to the parsed letter
                    val letterPosition = letterAdapter?.getPosition(letter) ?: 0
                    binding.letterSpinner.setSelection(letterPosition)
                } else {
                    Log.e("PlantDropdownAdapter", "Invalid number extracted from TableName: '$numberString'")
                    resetSpinners(binding)
                }
            } else if (currentTableName.isEmpty()) {
                Log.e("PlantDropdownAdapter", "Empty TableName, resetting spinners.")
                resetSpinners(binding)
            }
        }
    }

    private fun resetSpinners(binding: PlantBindingWrapper) {
        // Reset spinners to default (empty) if TableName is invalid or empty
        binding.letterSpinner.setSelection(0)
        updateNumberSpinner(binding, "", null)
        binding.numberSpinner.setSelection(0)
    }

    fun updateNumberSpinner(binding: PlantBindingWrapper, selectedLetter: String, desiredNumber: Int?) {
        val numberArrayResId = when (selectedLetter) {
            "A", "B", "C", "E" -> R.array.number_array_1_to_6
            "D", "G" -> R.array.number_array_1_to_16
            "F" -> R.array.number_array_1_to_24
            "H" -> R.array.number_array_1_to_19
            "I" -> R.array.number_array_1_to_2
            "" -> R.array.empty_number_array // Ensure this array exists with an empty string
            else -> R.array.number_array // Default fallback
        }

        // Retrieve the number array and add an empty first item
        val numbers = binding.letterSpinner.context.resources.getStringArray(numberArrayResId).toMutableList()
        numbers.add(0, "") // Add empty option at the beginning

        // Create the new number adapter with the updated list
        val numberAdapter = ArrayAdapter(
            binding.letterSpinner.context,
            android.R.layout.simple_dropdown_item_1line,
            numbers
        )

        // Set the adapter for the number spinner
        binding.numberSpinner.adapter = numberAdapter

        // Logging to verify the adapter setup
        Log.d("PlantDropdownAdapter", "Number spinner adapter set with items: $numbers")

        // Now, set the selection
        binding.numberSpinner.post {
            if (desiredNumber != null) {
                val numberPosition = desiredNumber // Because position 0 is empty, position 1 is "1", etc.

                if (numberPosition in 1 until binding.numberSpinner.adapter.count) {
                    binding.numberSpinner.setSelection(numberPosition)

                    // Logging the selection
                    Log.d(
                        "PlantDropdownAdapter",
                        "Setting number spinner to position: $numberPosition with value: ${binding.numberSpinner.getItemAtPosition(numberPosition)}"
                    )
                } else {
                    Log.e("PlantDropdownAdapter", "Number position out of bounds: $numberPosition")
                    binding.numberSpinner.setSelection(0) // Default to empty if out of bounds
                }
            } else {
                // Optionally set to empty
                binding.numberSpinner.setSelection(0)
                Log.d("PlantDropdownAdapter", "Desired number is null, setting number spinner to empty.")
            }
        }
    }

    private suspend fun handleFamilySelection(context: Context, familyName: String, binding: PlantBindingWrapper) {
        withContext(Dispatchers.IO) {
            val familyExists = PlantRepository.isFamilyExists(context, familyName)
            withContext(Dispatchers.Main) {
                if (!familyExists) {
                    binding.speciesAutoCompleteTextView.text.clear()
                    binding.subspeciesAutoCompleteTextView.text.clear()
                    clearSpeciesAdapter(binding.speciesAutoCompleteTextView)
                    clearSubspeciesAdapter(binding.subspeciesAutoCompleteTextView)
                    clearMotherPlantIdAdapter(binding)
                } else {
                    val hasSpecies = updateSpeciesAdapter(context, familyName)
                    // Pass the current species value to ensure it is retained
                    applySpeciesAdapter(binding, hasSpecies, binding.speciesAutoCompleteTextView.text.toString())

                    val motherPlantsList = setupMotherPlantIdAdapter(context, familyName, binding)
                    if (motherPlantsList.isNotEmpty()) {
                        binding.mIdAutoCompleteTextView.setOnClickListener {
                            binding.mIdAutoCompleteTextView.showDropDown()
                        }
                    } else {
                        clearMotherPlantIdAdapter(binding)
                    }
                }
            }
        }
    }

    suspend fun updateSpeciesAdapter(context: Context, family: String): Boolean {
        Log.d("PlantDropdownAdapter", "Fetching unique species for family '$family' for dropdown setup")
        val species = withContext(Dispatchers.IO) {
            PlantRepository.getSpeciesByFamily(context, family)
        }
        Log.d("PlantDropdownAdapter", "Setting up Species dropdown with ${species.size} items")

        speciesAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            species
        )

        return species.isNotEmpty()
    }

    fun applySpeciesAdapter(binding: PlantBindingWrapper, hasSpecies: Boolean, currentSpecies: String? = null) {
        binding.runOnUiThread {
            binding.speciesAutoCompleteTextView.setAdapter(speciesAdapter)
            // Only set the text if currentSpecies is provided
            currentSpecies?.let {
                binding.speciesAutoCompleteTextView.setText(it, false)
            }
            binding.speciesAutoCompleteTextView.threshold = 0
            binding.speciesAutoCompleteTextView.setOnClickListener {
                binding.speciesAutoCompleteTextView.showDropDown()
            }

            if (!hasSpecies) {
                binding.speciesAutoCompleteTextView.text.clear()
            }

            binding.speciesAutoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val speciesName = binding.speciesAutoCompleteTextView.text.toString()
                    binding.lifecycleScope.launch {
                        handleSpeciesSelection(
                            binding.familyAutoCompleteTextView.context,
                            binding.familyAutoCompleteTextView.text.toString(),
                            speciesName,
                            binding
                        )
                    }
                }
            }

            binding.speciesAutoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
                val speciesName = binding.speciesAutoCompleteTextView.text.toString()
                binding.lifecycleScope.launch {
                    handleSpeciesSelection(
                        binding.familyAutoCompleteTextView.context,
                        binding.familyAutoCompleteTextView.text.toString(),
                        speciesName,
                        binding
                    )
                }
            }
        }
    }

    private suspend fun handleSpeciesSelection(context: Context, familyName: String, speciesName: String, binding: PlantBindingWrapper) {
        withContext(Dispatchers.IO) {
            val speciesExists = PlantRepository.isSpeciesExists(context, familyName, speciesName)
            withContext(Dispatchers.Main) {
                if (!speciesExists) {
                    binding.subspeciesAutoCompleteTextView.text.clear()
                    clearSubspeciesAdapter(binding.subspeciesAutoCompleteTextView)
                } else {
                    val hasSubspecies = updateSubspeciesAdapter(context, familyName, speciesName)
                    applySubspeciesAdapter(binding, hasSubspecies)
                }
            }
        }
    }

    suspend fun updateSubspeciesAdapter(context: Context, family: String, species: String): Boolean {
        Log.d("PlantDropdownAdapter", "Fetching unique subspecies for family '$family' and species '$species' for dropdown setup")
        val subspecies = withContext(Dispatchers.IO) {
            PlantRepository.getSubspeciesByFamilyAndSpecies(context, family, species)
        }
        Log.d("PlantDropdownAdapter", "Setting up Subspecies dropdown with ${subspecies.size} items")

        subspeciesAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            subspecies
        )

        return subspecies.isNotEmpty()
    }

    fun applySubspeciesAdapter(binding: PlantBindingWrapper, hasSubspecies: Boolean) {
        binding.runOnUiThread {
            binding.subspeciesAutoCompleteTextView.setAdapter(subspeciesAdapter)
            binding.subspeciesAutoCompleteTextView.setText(if (hasSubspecies) "" else "", false)
            binding.subspeciesAutoCompleteTextView.threshold = 0
            binding.subspeciesAutoCompleteTextView.setOnClickListener {
                binding.subspeciesAutoCompleteTextView.showDropDown()
            }

            if (!hasSubspecies) {
                binding.subspeciesAutoCompleteTextView.text.clear()
            }
        }
    }

    suspend fun setupMotherPlantIdAdapter(context: Context, family: String, binding: PlantBindingWrapper): List<String> {
        Log.d("PlantDropdownAdapter", "Fetching mother plants for family '$family'")
        val motherPlants = withContext(Dispatchers.IO) {
            PlantRepository.getMotherPlantsByFamily(family)
        }
        Log.d("PlantDropdownAdapter", "Setting up Mother Plant dropdown with ${motherPlants.size} items")

        val formattedMotherPlants = motherPlants.map { plant ->
            "Stock ID: ${plant.StockID}, ${plant.NameConcat}, Stock Price: ${plant.StockPrice} (Qty: ${plant.StockQty})"
        }
        Log.d("PlantDropdownAdapter", "Formatted Mother plants: ${formattedMotherPlants.size}")

        motherPlantAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            formattedMotherPlants
        )

        binding.mIdAutoCompleteTextView.setAdapter(motherPlantAdapter)
        return formattedMotherPlants
    }

    suspend fun setupFatherPlantIdAdapter(context: Context, binding: PlantBindingWrapper): List<String> {
        Log.d("PlantDropdownAdapter", "Fetching father plants")
        val fatherPlants = withContext(Dispatchers.IO) {
            PlantRepository.getFatherPlants()
        }
        Log.d("PlantDropdownAdapter", "Setting up Father Plant dropdown with ${fatherPlants.size} items")

        val formattedFatherPlants = fatherPlants.map { plant ->
            "Stock ID: ${plant.StockID}, ${plant.NameConcat}, Stock Price: ${plant.StockPrice} (Qty: ${plant.StockQty})"
        }
        Log.d("PlantDropdownAdapter", "Formatted Father plants: ${formattedFatherPlants.size}")

        fatherPlantAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            formattedFatherPlants
        )

        binding.fIdAutoCompleteTextView.setAdapter(fatherPlantAdapter)
        return formattedFatherPlants
    }

    fun applyMotherPlantAdapter(binding: PlantBindingWrapper, motherPlants: List<String>) {
        binding.runOnUiThread {
            motherPlantAdapter = ArrayAdapter(
                binding.familyAutoCompleteTextView.context, // Corrected to use context from the AutoCompleteTextView
                android.R.layout.simple_dropdown_item_1line,
                motherPlants
            )
            binding.mIdAutoCompleteTextView.setAdapter(motherPlantAdapter)
            // Show dropdown only when the user clicks on it
            binding.mIdAutoCompleteTextView.setOnClickListener {
                binding.mIdAutoCompleteTextView.showDropDown()
            }
        }
    }

    fun applyFatherPlantAdapter(binding: PlantBindingWrapper, fatherPlants: List<String>) {
        binding.runOnUiThread {
            fatherPlantAdapter = ArrayAdapter(
                binding.fIdAutoCompleteTextView.context, // Use context from fIdAutoCompleteTextView
                android.R.layout.simple_dropdown_item_1line,
                fatherPlants
            )
            binding.fIdAutoCompleteTextView.setAdapter(fatherPlantAdapter)
        }
    }

    private fun clearMotherPlantIdAdapter(binding: PlantBindingWrapper) {
        val emptyAdapter = ArrayAdapter(binding.familyAutoCompleteTextView.context, android.R.layout.simple_dropdown_item_1line, emptyList<String>())
        binding.mIdAutoCompleteTextView.setAdapter(emptyAdapter)
    }

    private fun clearSpeciesAdapter(view: AutoCompleteTextView) {
        val emptyAdapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, emptyList<String>())
        view.setAdapter(emptyAdapter)
    }

    private fun clearSubspeciesAdapter(view: AutoCompleteTextView) {
        val emptyAdapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, emptyList<String>())
        view.setAdapter(emptyAdapter)
    }

    // Wrapper interface to abstract common binding functions
    interface PlantBindingWrapper {
        val familyAutoCompleteTextView: AutoCompleteTextView
        val speciesAutoCompleteTextView: AutoCompleteTextView
        val subspeciesAutoCompleteTextView: AutoCompleteTextView
        val mIdAutoCompleteTextView: AutoCompleteTextView
        val fIdAutoCompleteTextView: AutoCompleteTextView
        val plantStatusAutoCompleteTextView: AutoCompleteTextView
        val letterSpinner: Spinner
        val numberSpinner: Spinner
        val lifecycleScope: LifecycleCoroutineScope
        fun runOnUiThread(action: () -> Unit)
    }

    // Wrapper for EditPlantBinding
    class EditPlantBindingWrapper(
        private val binding: ActivityEditPlantBinding
    ) : PlantBindingWrapper {
        override val familyAutoCompleteTextView get() = binding.familyAutoCompleteTextView
        override val speciesAutoCompleteTextView get() = binding.speciesAutoCompleteTextView
        override val subspeciesAutoCompleteTextView get() = binding.subspeciesAutoCompleteTextView
        override val mIdAutoCompleteTextView get() = binding.mIdAutoCompleteTextView
        override val fIdAutoCompleteTextView get() = binding.fIdAutoCompleteTextView
        override val plantStatusAutoCompleteTextView get() = binding.plantStatusAutoCompleteTextView
        override val letterSpinner get() = binding.letterSpinner
        override val numberSpinner get() = binding.numberSpinner

        override val lifecycleScope: LifecycleCoroutineScope
            get() = (binding.root.context as? AppCompatActivity)?.lifecycleScope
                ?: throw IllegalStateException("Unable to get lifecycleScope")

        override fun runOnUiThread(action: () -> Unit) {
            (binding.root.context as? android.app.Activity)?.runOnUiThread(action)
        }
    }

    // Wrapper for NewPlantBinding
    class NewPlantBindingWrapper(
        private val binding: ActivityNewPlantBinding,
        override val lifecycleScope: LifecycleCoroutineScope
    ) : PlantBindingWrapper {
        override val familyAutoCompleteTextView get() = binding.familyAutoCompleteTextView
        override val speciesAutoCompleteTextView get() = binding.speciesAutoCompleteTextView
        override val subspeciesAutoCompleteTextView get() = binding.subspeciesAutoCompleteTextView
        override val mIdAutoCompleteTextView get() = binding.mIdAutoCompleteTextView
        override val fIdAutoCompleteTextView get() = binding.fIdAutoCompleteTextView
        override val plantStatusAutoCompleteTextView get() = binding.plantStatusAutoCompleteTextView
        override val letterSpinner get() = binding.letterSpinner
        override val numberSpinner get() = binding.numberSpinner
        override fun runOnUiThread(action: () -> Unit) {
            (binding.root.context as? android.app.Activity)?.runOnUiThread(action)
        }
    }
}