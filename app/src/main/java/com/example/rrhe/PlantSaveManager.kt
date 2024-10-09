package com.example.rrhe

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.rrhe.databinding.ActivityEditPlantBinding
import com.example.rrhe.databinding.ActivityNewPlantBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PlantSaveManager {
    internal var isPhotoUploading = false

    private var currentDatePickerType: DatePickerType? = null

    sealed class BindingWrapper {
        data class Edit(val binding: ActivityEditPlantBinding) : BindingWrapper()
        data class New(val binding: ActivityNewPlantBinding) : BindingWrapper()
    }

    enum class DatePickerType {
        POLLINATE_DATE,
        SEEDS_PLANTED,
        SEEDS_HARVEST
    }

    private fun setCurrentDatePickerType(type: DatePickerType) {
        currentDatePickerType = type
    }

    private fun getCurrentDatePickerType(): String {
        return currentDatePickerType?.name ?: throw IllegalStateException("DatePickerType is not set")
    }

    fun updateUI(binding: ActivityEditPlantBinding, plant: Plant) {
        binding.plantedTextView.text = binding.root.context.getString(
            R.string.date_range_format,
            plant.PlantedStart ?: "",
            plant.PlantedEnd ?: ""
        )
        binding.pollinateDateTextView.text = plant.PollinateDate ?: ""
        binding.seedsPlantedTextView.text = plant.SeedsPlanted ?: ""
        binding.seedsHarvestTextView.text = plant.SeedsHarvest ?: ""
    }

    fun setupDatePickers(
        binding: BindingWrapper,
        dateFormat: SimpleDateFormat,
        calendar: Calendar,
        currentPlant: Plant?,
        updateDatePickerType: (DatePickerType) -> Unit
    ) {
        val dateSetListenerForRange = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            val startDate = dateFormat.format(calendar.time)

            DatePickerDialog(
                binding.rootContext(),
                { _, endYear, endMonth, endDayOfMonth ->
                    calendar.set(endYear, endMonth, endDayOfMonth)
                    val endDate = dateFormat.format(calendar.time)
                    updatePlantedDates(binding, startDate, endDate)
                    currentPlant?.PlantedStart = startDate
                    currentPlant?.PlantedEnd = endDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            val selectedDate = dateFormat.format(calendar.time)
            when (DatePickerType.valueOf(getCurrentDatePickerType())) {
                DatePickerType.POLLINATE_DATE -> {
                    binding.getPollinateTextView().text = selectedDate
                    currentPlant?.PollinateDate = selectedDate
                }
                DatePickerType.SEEDS_PLANTED -> {
                    binding.getSeedsPlantedTextView().text = selectedDate
                    currentPlant?.SeedsPlanted = selectedDate
                }
                DatePickerType.SEEDS_HARVEST -> {
                    binding.getSeedsHarvestTextView().text = selectedDate
                    currentPlant?.SeedsHarvest = selectedDate
                }
            }
        }

        binding.getPlantedTextView().setOnClickListener {
            DatePickerDialog(
                binding.rootContext(),
                dateSetListenerForRange,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.getPollinateTextView().setOnClickListener {
            updateDatePickerType(DatePickerType.POLLINATE_DATE)
            setCurrentDatePickerType(DatePickerType.POLLINATE_DATE)
            DatePickerDialog(
                binding.rootContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.getSeedsPlantedTextView().setOnClickListener {
            updateDatePickerType(DatePickerType.SEEDS_PLANTED)
            setCurrentDatePickerType(DatePickerType.SEEDS_PLANTED)
            DatePickerDialog(
                binding.rootContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.getSeedsHarvestTextView().setOnClickListener {
            updateDatePickerType(DatePickerType.SEEDS_HARVEST)
            setCurrentDatePickerType(DatePickerType.SEEDS_HARVEST)
            DatePickerDialog(
                binding.rootContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun BindingWrapper.rootContext(): Context {
        return when (this) {
            is BindingWrapper.Edit -> binding.root.context
            is BindingWrapper.New -> binding.root.context
        }
    }

    private fun BindingWrapper.getPlantedTextView() = when (this) {
        is BindingWrapper.Edit -> binding.plantedTextView
        is BindingWrapper.New -> binding.plantedTextView
    }

    private fun BindingWrapper.getPollinateTextView() = when (this) {
        is BindingWrapper.Edit -> binding.pollinateDateTextView
        is BindingWrapper.New -> binding.pollinateDateTextView
    }

    private fun BindingWrapper.getSeedsPlantedTextView() = when (this) {
        is BindingWrapper.Edit -> binding.seedsPlantedTextView
        is BindingWrapper.New -> binding.seedsPlantedTextView
    }

    private fun BindingWrapper.getSeedsHarvestTextView() = when (this) {
        is BindingWrapper.Edit -> binding.seedsHarvestTextView
        is BindingWrapper.New -> binding.seedsHarvestTextView
    }

    private fun updatePlantedDates(binding: BindingWrapper, startDate: String, endDate: String) {
        binding.getPlantedTextView().text = binding.rootContext().getString(
            R.string.date_range_format, startDate, endDate
        )
    }

    private fun convertEmptyToNull(value: String?): String? {
        return if (value.isNullOrBlank()) null else value
    }

    private suspend fun performLocalSave(
        context: Context,
        binding: ActivityEditPlantBinding?,
        newBinding: ActivityNewPlantBinding?,
        currentPlant: Plant?,
        isEditMode: Boolean,
        plantBindingWrapper: PlantValueCalculator.PlantBindingWrapper,
        tempStockID: Int? = null // Add tempStockID as an optional parameter for new plants
    ): Plant {
        // Use tempStockID for new plants, and normal StockID for existing plants
        val stockID = if (isEditMode) {
            currentPlant?.StockID ?: throw IllegalStateException("Editing mode but StockID is null")
        } else {
            tempStockID ?: throw IllegalStateException("Temp StockID is not available for new plant")
        }

        val stockPrice = plantBindingWrapper.getStockPrice().toIntOrNull() ?: 0
        val stockQty = plantBindingWrapper.getStockQty().toIntOrNull() ?: 0

        // Fetch calculated values from PlantBindingWrapper
        val totalValue = stockPrice * stockQty

        val usdValue = plantBindingWrapper.getUSD().toIntOrNull() ?: 0
        val eurValue = plantBindingWrapper.getEUR().toIntOrNull() ?: 0

        Log.d("PlantSaveManager", "Calculated values before saving: totalValue=$totalValue, usdValue=$usdValue, eurValue=$eurValue")
        Log.d("PlantSaveManager", "USD fetched from binding before saving: $usdValue")
        Log.d("PlantSaveManager", "EUR fetched from binding before saving: $eurValue")

        // Upload photos asynchronously via PhotoManager and handle null cases
        val photo1Path = (binding?.photoEdit1?.tag as? Uri ?: newBinding?.photoEdit1?.tag as? Uri)?.let {
            // Determine whether to use stockID or tempStockID
            val idToUse = if (isEditMode) stockID else tempStockID ?: throw IllegalStateException("Temp StockID is not available for new plant")

            // Call the PhotoManager function with the correct parameters
            PhotoManager.uploadPhotoToServer(it, context, idToUse, 1, isEditMode, tempStockID!!)
        }

        val photo2Path = (binding?.photoEdit2?.tag as? Uri ?: newBinding?.photoEdit2?.tag as? Uri)?.let {
            val idToUse = if (isEditMode) stockID else tempStockID ?: throw IllegalStateException("Temp StockID is not available for new plant")

            PhotoManager.uploadPhotoToServer(it, context, idToUse, 2, isEditMode, tempStockID!!)
        }

        val photo3Path = (binding?.photoEdit3?.tag as? Uri ?: newBinding?.photoEdit3?.tag as? Uri)?.let {
            val idToUse = if (isEditMode) stockID else tempStockID ?: throw IllegalStateException("Temp StockID is not available for new plant")

            PhotoManager.uploadPhotoToServer(it, context, idToUse, 3, isEditMode, tempStockID!!)
        }

        val photo4Path = (binding?.photoEdit4?.tag as? Uri ?: newBinding?.photoEdit4?.tag as? Uri)?.let {
            val idToUse = if (isEditMode) stockID else tempStockID ?: throw IllegalStateException("Temp StockID is not available for new plant")

            PhotoManager.uploadPhotoToServer(it, context, idToUse, 4, isEditMode, tempStockID!!)
        }

        val motherPlantStockID = binding?.mIdAutoCompleteTextView?.text?.toString()
            ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()
            ?: newBinding?.mIdAutoCompleteTextView?.text?.toString()
                ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()

        val fatherPlantStockID = binding?.fIdAutoCompleteTextView?.text?.toString()
            ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()
            ?: newBinding?.fIdAutoCompleteTextView?.text?.toString()
                ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()

        // Only assert that StockID is negative in new plant creation
        if (!isEditMode) {
            Log.d("PlantSaveManager", "Using Temp StockID: $stockID")
            assert(stockID < 0) { "Temp StockID should always be negative, found: $stockID" }
        }

        // Get today's date and the date three weeks later
        val today = Calendar.getInstance().time
        val threeWeeksLater = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, 3)
        }.time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentUserName = sharedPreferences.getString("userName", null)

        // Process the values with potential NULL conversion for the main database
        val plant = if (isEditMode && currentPlant != null) {

            // Get the selected letter and number from the spinners
            val selectedLetter = binding?.letterSpinner?.selectedItem?.toString() ?: ""
            val selectedNumber = binding?.numberSpinner?.selectedItem?.toString() ?: ""
            val tableName = if (selectedLetter.isNotEmpty() && selectedNumber.isNotEmpty()) {
                "$selectedLetter$selectedNumber"
            } else {
                null
            }

            currentPlant.copy(
                Family = convertEmptyToNull(binding?.familyAutoCompleteTextView?.text.toString()),
                Species = convertEmptyToNull(binding?.speciesAutoCompleteTextView?.text.toString()),
                Subspecies = convertEmptyToNull(binding?.subspeciesAutoCompleteTextView?.text.toString()),
                M_ID = motherPlantStockID ?: currentPlant.M_ID,
                F_ID = fatherPlantStockID ?: currentPlant.F_ID,
                TableName = convertEmptyToNull(tableName),
                StockQty = stockQty,
                StockPrice = stockPrice,
                TotalValue = totalValue,
                USD = binding?.usdEditText?.text.toString().toIntOrNull() ?: currentPlant.USD,
                EUR = binding?.eurEditText?.text.toString().toIntOrNull() ?: currentPlant.EUR,
                PurchasePrice = binding?.purchasePriceEditText?.text.toString().toIntOrNull() ?: 0,
                PlantDescription = convertEmptyToNull(binding?.plantDescriptionEditText?.text.toString()),
                ThaiName = convertEmptyToNull(binding?.thaiNameText?.text.toString()),
                NameConcat = convertEmptyToNull(binding?.nameConcatText?.text.toString()) ?: currentPlant.NameConcat,
                TraySize = convertEmptyToNull(binding?.traySizeAutoCompleteTextView?.text.toString()),
                Grams = binding?.gramsEditText?.text.toString().toIntOrNull() ?: 0,
                PlantStatus = convertEmptyToNull(binding?.plantStatusAutoCompleteTextView?.text.toString()),
                StatusNote = convertEmptyToNull(binding?.statusNoteEditText?.text.toString()),
                Mother = if (binding?.motherSwitch?.isChecked == true) 1 else currentPlant.Mother ?: 0,
                Website = if (binding?.websiteSwitch?.isChecked == true) 1 else currentPlant.Website ?: 0,
                Variegated = if (binding?.variegatedSwitch?.isChecked == true) 1 else currentPlant.Variegated ?: 0,
                Photo1 = convertEmptyToNull((photo1Path ?: currentPlant.Photo1).toString()),
                Photo2 = convertEmptyToNull((photo2Path ?: currentPlant.Photo2).toString()),
                Photo3 = convertEmptyToNull((photo3Path ?: currentPlant.Photo3).toString()),
                Photo4 = convertEmptyToNull((photo4Path ?: currentPlant.Photo4).toString()),
                LastEditedBy = currentUserName,
                Stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            ).ensureNonNullValues()
        } else {

            // Construct TableName from the spinners
            val selectedLetter = newBinding?.letterSpinner?.selectedItem?.toString() ?: ""
            val selectedNumber = newBinding?.numberSpinner?.selectedItem?.toString() ?: ""
            val tableName = if (selectedLetter.isNotEmpty() && selectedNumber.isNotEmpty()) {
                "$selectedLetter$selectedNumber"
            } else {
                null
            }

            Plant(
                StockID = stockID,
                Family = convertEmptyToNull(newBinding?.familyAutoCompleteTextView?.text.toString()),
                Species = convertEmptyToNull(newBinding?.speciesAutoCompleteTextView?.text.toString()),
                Subspecies = convertEmptyToNull(newBinding?.subspeciesAutoCompleteTextView?.text.toString()),
                M_ID = motherPlantStockID,
                F_ID = fatherPlantStockID,
                StockQty = stockQty,
                StockPrice = stockPrice,
                TotalValue = totalValue,
                USD = usdValue,
                EUR = eurValue,
                PurchasePrice = newBinding?.purchasePriceEditText?.text.toString().toIntOrNull() ?: 0,
                PlantDescription = convertEmptyToNull(newBinding?.plantDescriptionEditText?.text.toString()),
                NameConcat = convertEmptyToNull(newBinding?.nameConcatText?.text.toString()),
                ThaiName = convertEmptyToNull(newBinding?.thaiNameText?.text.toString()),
                TableName = convertEmptyToNull(tableName),
                TraySize = convertEmptyToNull(newBinding?.traySizeAutoCompleteTextView?.text.toString()),
                Grams = newBinding?.gramsEditText?.text.toString().toIntOrNull() ?: 0,
                PlantStatus = convertEmptyToNull(newBinding?.plantStatusAutoCompleteTextView?.text.toString()),
                StatusNote = convertEmptyToNull(newBinding?.statusNoteEditText?.text.toString()),
                Mother = if (newBinding?.motherSwitch?.isChecked == true) 1 else 0,
                Website = if (newBinding?.websiteSwitch?.isChecked == true) 1 else 0,
                Variegated = if (newBinding?.variegatedSwitch?.isChecked == true) 1 else 0,
                Photo1 = convertEmptyToNull(photo1Path.toString()),
                Photo2 = convertEmptyToNull(photo2Path.toString()),
                Photo3 = convertEmptyToNull(photo3Path.toString()),
                Photo4 = convertEmptyToNull(photo4Path.toString()),
                AddedBy = currentUserName,
                LastEditedBy = null,
                PlantedStart = dateFormat.format(today), // Set PlantedStart to today's date
                PlantedEnd = dateFormat.format(threeWeeksLater), // Set PlantedEnd to three weeks later
                PollinateDate = newBinding?.pollinateDateTextView?.text?.toString().takeIf { !it.isNullOrBlank() },
                SeedsPlanted = newBinding?.seedsPlantedTextView?.text?.toString().takeIf { !it.isNullOrBlank() },
                SeedsHarvest = newBinding?.seedsHarvestTextView?.text?.toString().takeIf { !it.isNullOrBlank() },
                Weight = null,
                PhotoLink1 = null,
                PhotoLink2 = null,
                PhotoLink3 = null,
                PhotoLink4 = null,
                TrayQty = null,
                Stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            ).ensureNonNullValues()
        }

        // Log USD and EUR values before saving the plant
        Log.d("PlantSaveManager", "Final plant before saving: USD=${plant.USD}, EUR=${plant.EUR}")
        Log.d("PlantSaveManager", if (isEditMode) "Updating existing plant: $plant" else "Saving new plant: $plant")

        if (!isEditMode) {
            PlantRepository.saveNewPlantLocally(context, plant)
        } else {
            PlantRepository.updatePlant(plant)
        }

        return plant
    }

    private suspend fun syncWithDatabase(plant: Plant?, activity: AppCompatActivity) {
        plant?.let {
            Log.d("PlantSaveManager", "Syncing with the database")

            // This can stay as is if it checks whether the plant has unique entries
            PlantRepository.updateUniqueEntries(
                activity.applicationContext,
                it.Family.toString(),
                it.Species.toString(),
                it.Subspecies.toString()
            )

            // Check inside the sync logic whether the plant is new or existing
            if (it.StockID != null && it.StockID!! > 0) {
                Log.d("PlantSaveManager", "Calling savePlantUpdate for existing plant")
                PlantUpdateManager.savePlantUpdate(activity.applicationContext, it)
                showToast(activity, "Plant changes saved")
            } else {
                showToast(activity, "New plant saved")
            }

            val resultIntent = Intent().apply {
                putExtra("updated", it.StockID != null && it.StockID!! > 0) // check if it's updated or new
                putExtra("plant", it)
            }
            activity.setResult(AppCompatActivity.RESULT_OK, resultIntent)
            activity.finish()

            val isConnected = PlantRepository.isMainDatabaseConnected.value ?: false
            withContext(Dispatchers.Main) {
                if (isConnected) {
                    showToast(activity, "Merging changes")
                } else {
                    showToast(activity, "Connect to main database to merge changes")
                }
            }

            SyncManager.syncOnUserAction()
        }
    }

    fun savePlantLocallyAndSync(
        context: Context,
        binding: ActivityEditPlantBinding? = null,
        newBinding: ActivityNewPlantBinding? = null,
        currentPlant: Plant?,
        lifecycleScope: LifecycleCoroutineScope,
        activity: AppCompatActivity,
        isEditMode: Boolean,
        tempStockID: Int? = null // Add tempStockID as a parameter
    ) {
        Log.d("PlantSaveManager", "isEditMode: $isEditMode at the start of savePlantLocallyAndSync")

        if (isEditMode && isPhotoUploading) {
            Log.e("PlantSaveManager", "Cannot proceed: Photo is still uploading in edit mode.")
            lifecycleScope.launch(Dispatchers.Main) {
                showToast(activity, "Please wait until the photo is uploaded")
            }
            return
        }

        Log.d("PlantSaveManager", "Proceeding to save plant. isEditMode=$isEditMode")

        val plantBindingWrapper = when {
            isEditMode && binding != null -> PlantValueCalculator.EditPlantBindingWrapper(binding)
            !isEditMode && newBinding != null -> PlantValueCalculator.NewPlantBindingWrapper(newBinding)
            else -> throw IllegalStateException("Binding is missing")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    showToast(context, "Starting plant save process...")
                }

                // Recalculate values for either new or existing plant
                if (isEditMode) {
                    binding?.let {
                        withContext(Dispatchers.Main) {
                            PlantValueCalculator.recalculateValues(it, lifecycleScope, context)
                        }
                    }
                } else {
                    newBinding?.let {
                        withContext(Dispatchers.Main) {
                            PlantValueCalculator.recalculateValues(it, lifecycleScope, context)
                        }
                    }
                }

                delay(1000) // Optional delay

                // Perform local save (new or existing plants), pass tempStockID for new plants
                val plant = performLocalSave(
                    context = context,
                    binding = binding,
                    newBinding = newBinding,
                    currentPlant = currentPlant,
                    isEditMode = isEditMode,
                    plantBindingWrapper = plantBindingWrapper,
                    tempStockID = tempStockID // Ensure tempStockID is passed here
                )

                activity.finish()

                if (!isEditMode) {
                    Log.d("PlantSaveManager", "Syncing new plant with main database")
                    SyncManager.syncNewPlant(plant) { newStockID ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            // Upload cached photos with correct StockID
                            uploadCachedPhotosWithBinding(
                                newStockID = newStockID,
                                binding = binding,
                                newBinding = newBinding,
                                activity = activity,
                                lifecycleScope = lifecycleScope,
                                tempStockID = plant.StockID,
                                isEditMode = false
                            )
                        }
                    }
                } else {
                    Log.d("PlantSaveManager", "Final sync for an existing plant")
                    syncWithDatabase(plant, activity)
                }

            } catch (e: Exception) {
                Log.e("PlantSaveManager", "Error saving plant: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast(activity, "Error saving plant: ${e.message}")
                }
            }
        }
    }

    private fun navigateToEditPlantScreen(activity: AppCompatActivity, stockID: Int) {
        Log.d("PlantSaveManager", "Navigating to Edit Plant screen with StockID: $stockID")
        val intent = Intent(activity, EditPlantActivity::class.java).apply {
            putExtra("stockID", stockID)
            putExtra("isTempID", false) // Indicate that this is not a temp StockID
        }
        activity.startActivity(intent)
        activity.finish() // Finish the current activity to ensure we leave the New Plant screen
    }

    private suspend fun uploadCachedPhotosWithBinding(
        newStockID: Int,
        binding: ActivityEditPlantBinding?,
        newBinding: ActivityNewPlantBinding?,
        activity: AppCompatActivity,
        lifecycleScope: LifecycleCoroutineScope,
        tempStockID: Int?,  // For new plants
        isEditMode: Boolean // To distinguish between new and existing plants
    ) {
        try {
            // Get the correct PlantBinding for either Edit or New Plant
            val plantBinding = when {
                binding != null -> PhotoManager.getPlantBinding(binding)
                newBinding != null -> PhotoManager.getPlantBinding(newBinding)
                else -> throw IllegalStateException("Both bindings cannot be null")
            }

            // Determine which stock ID to use
            val stockIDToUse = if (isEditMode) newStockID else tempStockID
                ?: throw IllegalStateException("Temp StockID is not available for new plant")

            // Upload cached photos with the determined stock ID
            PhotoManager.uploadCachedPhotos(
                stockID = stockIDToUse,  // Use stockIDToUse (newStockID or tempStockID)
                binding = plantBinding,  // Pass PlantBinding (Edit or New)
                activity = activity,  // Pass AppCompatActivity
                scope = lifecycleScope,  // Pass LifecycleCoroutineScope for coroutine handling
                isEditMode = isEditMode,  // Pass isEditMode to indicate if it's an edit or a new plant
                tempStockID = tempStockID  // Pass tempStockID for new plants
            )

        } catch (e: Exception) {
            // Log the error and show a toast on the main thread
            Log.e("PlantSaveManager", "Error uploading cached photos: ${e.message}", e)
            withContext(Dispatchers.Main) {
                showToast(activity, "Error uploading photos: ${e.message}")
            }
        }
    }

    suspend fun generateTempStockID(): Int = withContext(Dispatchers.IO) {
        var tempStockID: Int
        val stockIDList = PlantRepository.getAllStockIDs() // Retrieve all existing StockIDs

        if (stockIDList.isEmpty()) {
            Log.d("StockIDGeneration", "No existing StockIDs found in local database (possibly a fresh install).")
        } else {
            Log.d("StockIDGeneration", "Existing StockIDs: $stockIDList")
        }

        var attempts = 0 // Count how many attempts it takes to generate a unique StockID

        do {
            tempStockID = -(1..Int.MAX_VALUE).random() // Generate a negative StockID
            attempts++
            Log.d("StockIDGeneration", "Attempt $attempts: Generated Temp StockID: $tempStockID")
        } while (stockIDList.contains(tempStockID)) // Check if it already exists in the local database

        Log.d("StockIDGeneration", "Unique Temp StockID generated after $attempts attempt(s): $tempStockID")

        tempStockID
    }
}