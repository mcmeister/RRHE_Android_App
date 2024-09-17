package com.example.rrhe

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import androidx.lifecycle.LifecycleCoroutineScope
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
    private var letterAdapter: ArrayAdapter<String>? = null
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
        Log.d("PlantDropdownAdapter", "Setting up Table Name dropdowns")

        letterAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            context.resources.getStringArray(R.array.letter_array)
        )

        numberAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            context.resources.getStringArray(R.array.number_array)
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
            // Apply the letter dropdown
            binding.letterSpinner.adapter = letterAdapter

            // Listener for letter spinner to update the number spinner
            binding.letterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedLetter = parent.getItemAtPosition(position) as String
                    updateNumberSpinner(binding, selectedLetter)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }

            // Apply the number dropdown based on the current table name
            if (currentTableName.length > 1) {
                val letter = currentTableName.substring(0, 1)
                val number = currentTableName.substring(1)

                binding.letterSpinner.setSelection(letterAdapter?.getPosition(letter) ?: 0)
                updateNumberSpinner(binding, letter)
                binding.numberSpinner.setSelection(numberAdapter?.getPosition(number) ?: 0)
            } else {
                // Default selection for new plant
                binding.letterSpinner.setSelection(0)
                updateNumberSpinner(binding, "A")
                binding.numberSpinner.setSelection(0)
            }
        }
    }

    private fun updateNumberSpinner(binding: PlantBindingWrapper, selectedLetter: String) {
        val numberArrayResId = when (selectedLetter) {
            "A", "B", "C", "E" -> R.array.number_array_1_to_6
            "D", "G" -> R.array.number_array_1_to_16
            "F" -> R.array.number_array_1_to_24
            "H" -> R.array.number_array_1_to_19
            "I" -> R.array.number_array_1_to_2
            else -> R.array.number_array // Default fallback, should not happen with defined letters
        }

        numberAdapter = ArrayAdapter(
            binding.letterSpinner.context,
            android.R.layout.simple_dropdown_item_1line,
            binding.letterSpinner.context.resources.getStringArray(numberArrayResId)
        )
        binding.numberSpinner.adapter = numberAdapter
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
        private val binding: ActivityEditPlantBinding,
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