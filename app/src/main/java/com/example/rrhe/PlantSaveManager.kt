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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

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
        isEditMode: Boolean
    ): Plant {
        val stockID = if (isEditMode) {
            currentPlant?.StockID ?: throw IllegalStateException("Editing mode but StockID is null")
        } else {
            generateTempStockID()
        }

        // Upload photos asynchronously via PhotoManager
        val photo1Path = (binding?.photoEdit1?.tag as? Uri ?: newBinding?.photoEdit1?.tag as? Uri)?.let {
            PhotoManager.uploadPhotoToServer(it, context, stockID, 1)
        }

        val photo2Path = (binding?.photoEdit2?.tag as? Uri ?: newBinding?.photoEdit2?.tag as? Uri)?.let {
            PhotoManager.uploadPhotoToServer(it, context, stockID, 2)
        }

        val photo3Path = (binding?.photoEdit3?.tag as? Uri ?: newBinding?.photoEdit3?.tag as? Uri)?.let {
            PhotoManager.uploadPhotoToServer(it, context, stockID, 3)
        }

        val photo4Path = (binding?.photoEdit4?.tag as? Uri ?: newBinding?.photoEdit4?.tag as? Uri)?.let {
            PhotoManager.uploadPhotoToServer(it, context, stockID, 4)
        }

        val motherPlantStockID = binding?.mIdAutoCompleteTextView?.text?.toString()
            ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()
            ?: newBinding?.mIdAutoCompleteTextView?.text?.toString()
                ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()

        val fatherPlantStockID = binding?.fIdAutoCompleteTextView?.text?.toString()
            ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()
            ?: newBinding?.fIdAutoCompleteTextView?.text?.toString()
                ?.split(",")?.getOrNull(0)?.split("Stock ID: ")?.getOrNull(1)?.toIntOrNull()

        Log.d("PlantSaveManager", "Generated StockID: $stockID")
        assert(stockID < 0) { "Temp StockID should always be negative, found: $stockID" }

        // Process the values with potential NULL conversion for the main database
        val plant = if (isEditMode && currentPlant != null) {
            currentPlant.copy(
                Family = convertEmptyToNull(binding?.familyAutoCompleteTextView?.text.toString().ifBlank { currentPlant.Family ?: "" }),
                Species = convertEmptyToNull(binding?.speciesAutoCompleteTextView?.text.toString().ifBlank { currentPlant.Species ?: "" }),
                Subspecies = convertEmptyToNull(binding?.subspeciesAutoCompleteTextView?.text.toString().ifBlank { currentPlant.Subspecies ?: "" }),
                M_ID = motherPlantStockID ?: currentPlant.M_ID,
                F_ID = fatherPlantStockID ?: currentPlant.F_ID,
                StockQty = binding?.stockQtyEditText?.text.toString().toIntOrNull() ?: currentPlant.StockQty,
                StockPrice = binding?.stockPriceEditText?.text.toString().toIntOrNull() ?: currentPlant.StockPrice,
                PurchasePrice = binding?.purchasePriceEditText?.text.toString().toIntOrNull() ?: currentPlant.PurchasePrice,
                PlantDescription = convertEmptyToNull(binding?.plantDescriptionEditText?.text.toString().ifBlank { currentPlant.PlantDescription ?: "" }),
                ThaiName = convertEmptyToNull(binding?.thaiNameText?.text.toString().ifBlank { currentPlant.ThaiName ?: "" }),
                TableName = convertEmptyToNull("${binding?.letterSpinner?.selectedItem}${binding?.numberSpinner?.selectedItem}".ifBlank { currentPlant.TableName ?: "" }),
                TraySize = convertEmptyToNull(binding?.traySizeEditText?.text.toString().ifBlank { currentPlant.TraySize ?: "" }),
                Grams = binding?.gramsEditText?.text.toString().toIntOrNull() ?: currentPlant.Grams,
                PlantStatus = convertEmptyToNull(binding?.plantStatusAutoCompleteTextView?.text.toString().ifBlank { currentPlant.PlantStatus ?: "" }),
                StatusNote = convertEmptyToNull(binding?.statusNoteEditText?.text.toString().ifBlank { currentPlant.StatusNote ?: "" }),
                Mother = if (binding?.motherSwitch?.isChecked == true) 1 else currentPlant.Mother ?: 0,
                Website = if (binding?.websiteSwitch?.isChecked == true) 1 else currentPlant.Website ?: 0,
                Photo1 = convertEmptyToNull(photo1Path ?: currentPlant.Photo1),
                Photo2 = convertEmptyToNull(photo2Path ?: currentPlant.Photo2),
                Photo3 = convertEmptyToNull(photo3Path ?: currentPlant.Photo3),
                Photo4 = convertEmptyToNull(photo4Path ?: currentPlant.Photo4),
                Stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            ).ensureNonNullValues()
        } else {
            Plant(
                StockID = stockID,
                Family = convertEmptyToNull(newBinding?.familyAutoCompleteTextView?.text.toString().ifBlank { "" }),
                Species = convertEmptyToNull(newBinding?.speciesAutoCompleteTextView?.text.toString().ifBlank { "" }),
                Subspecies = convertEmptyToNull(newBinding?.subspeciesAutoCompleteTextView?.text.toString().ifBlank { "" }),
                M_ID = motherPlantStockID,
                F_ID = fatherPlantStockID,
                StockQty = newBinding?.stockQtyEditText?.text.toString().toIntOrNull() ?: 0,
                StockPrice = newBinding?.stockPriceEditText?.text.toString().toIntOrNull() ?: 0,
                PurchasePrice = newBinding?.purchasePriceEditText?.text.toString().toIntOrNull() ?: 0,
                PlantDescription = convertEmptyToNull(newBinding?.plantDescriptionEditText?.text.toString().ifBlank { "" }),
                ThaiName = convertEmptyToNull(newBinding?.thaiNameText?.text.toString().ifBlank { "" }),
                TableName = convertEmptyToNull("${newBinding?.letterSpinner?.selectedItem}${newBinding?.numberSpinner?.selectedItem}".ifBlank { "" }),
                TraySize = convertEmptyToNull(newBinding?.traySizeEditText?.text.toString().ifBlank { "" }),
                Grams = newBinding?.gramsEditText?.text.toString().toIntOrNull(),
                PlantStatus = convertEmptyToNull(newBinding?.plantStatusAutoCompleteTextView?.text.toString().ifBlank { "" }),
                StatusNote = convertEmptyToNull(newBinding?.statusNoteEditText?.text.toString().ifBlank { "" }),
                Mother = if (newBinding?.motherSwitch?.isChecked == true) 1 else 0,
                Website = if (newBinding?.websiteSwitch?.isChecked == true) 1 else 0,
                Photo1 = convertEmptyToNull(photo1Path),
                Photo2 = convertEmptyToNull(photo2Path),
                Photo3 = convertEmptyToNull(photo3Path),
                Photo4 = convertEmptyToNull(photo4Path),
                AddedBy = null,
                LastEditedBy = null,
                PlantedStart = null,
                PlantedEnd = null,
                PollinateDate = null,
                SeedsPlanted = null,
                SeedsHarvest = null,
                TotalValue = null,
                USD = null,
                EUR = null,
                Weight = null,
                Variegated = null,
                PhotoLink1 = null,
                PhotoLink2 = null,
                PhotoLink3 = null,
                PhotoLink4 = null,
                NameConcat = null,
                TrayQty = null,
                Stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            ).ensureNonNullValues()
        }

        Log.d("PlantSaveManager", if (isEditMode) "Updating existing plant: $plant" else "Saving new plant: $plant")

        if (isEditMode) {
            PlantRepository.updatePlant(plant)
        } else {
            PlantRepository.saveNewPlantLocally(context, plant)
        }

        return plant
    }

    private suspend fun syncWithDatabase(plant: Plant?, activity: AppCompatActivity, isEditMode: Boolean) {
        plant?.let {
            Log.d("PlantSaveManager", if (isEditMode) "Updating unique entries in the database" else "Syncing new plant with the database")
            PlantRepository.updateUniqueEntries(
                activity.applicationContext,
                it.Family.toString(),
                it.Species.toString(),
                it.Subspecies.toString()
            )

            if (isEditMode) {
                PlantUpdateManager.savePlantUpdate(activity.applicationContext, it)
            }

            showToast(activity, if (isEditMode) "Plant changes saved" else "New plant saved")

            val resultIntent = Intent().apply {
                putExtra("updated", isEditMode)
                putExtra("plant", it)
            }
            activity.setResult(AppCompatActivity.RESULT_OK, resultIntent)
            activity.finish()

            val isConnected = PlantRepository.isMainDatabaseConnected.value ?: false
            if (isConnected) {
                showToast(activity, "Merging changes")
            } else {
                showToast(activity, "Connect to main database to merge changes")
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
        isEditMode: Boolean
    ) {
        if (isPhotoUploading) {
            showToast(activity, "Please wait until the photo is uploaded")
            return
        }

        val job = lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Perform the local save first
                val plant = performLocalSave(context, binding, newBinding, currentPlant, isEditMode)

                // Immediately navigate to the Plant Details screen with the temp StockID
                withContext(Dispatchers.Main) {
                    if (isEditMode) {
                        (activity as? EditPlantActivity)?.updateUI(plant)
                    } else {
                        plant.StockID?.let { navigateToPlantDetailsScreen(activity, it) }
                    }
                }

                // Sync with the main database
                if (!isEditMode) {
                    SyncManager.syncNewPlant(plant) { newStockID ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            updateUIWithNewStockID(activity, newStockID)
                            withContext(Dispatchers.IO) {
                                uploadCachedPhotosWithBinding(newStockID, binding, newBinding, activity, lifecycleScope)
                            }
                        }
                    }
                }

                // Perform the final sync with the database to ensure data consistency
                syncWithDatabase(plant, activity, isEditMode)

            } catch (e: Exception) {
                Log.e("PlantSaveManager", "Error saving plant: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast(activity, "Error saving plant: ${e.message}")
                }
            }
        }

        // Log detailed information when the coroutine job completes
        job.invokeOnCompletion { throwable ->
            when (throwable) {
                null -> {
                    Log.d("PlantSaveManager", "Job completed successfully.")
                }
                is CancellationException -> {
                    Log.e("PlantSaveManager", "Job was cancelled: ${throwable.message}")
                    throwable.printStackTrace()
                }

                else -> {
                    Log.e("PlantSaveManager", "Job failed with exception: ${throwable.message}")
                    throwable.printStackTrace()
                }
            }
        }
    }

    private fun navigateToPlantDetailsScreen(activity: AppCompatActivity, tempStockID: Int) {
        val intent = Intent(activity, PlantDetailsActivity::class.java).apply {
            putExtra("stockID", tempStockID)
            putExtra("isTempID", true) // Mark that this is a temp StockID
        }
        activity.startActivity(intent)
        activity.finish() // Optionally finish the current activity if needed
    }

    private fun updateUIWithNewStockID(activity: AppCompatActivity, newStockID: Int) {
        // Refresh the Plant Details screen with the new StockID
        (activity as? PlantDetailsActivity)?.apply {
            this.stockID = newStockID // Set the new stock ID
            refreshPlantDetails() // Refresh details with new stock ID
        }
    }

    private suspend fun uploadCachedPhotosWithBinding(
        newStockID: Int,
        binding: ActivityEditPlantBinding?,
        newBinding: ActivityNewPlantBinding?,
        activity: AppCompatActivity,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        try {
            val plantBinding = when {
                binding != null -> PhotoManager.getPlantBinding(binding)
                newBinding != null -> PhotoManager.getPlantBinding(newBinding)
                else -> throw IllegalStateException("Both bindings cannot be null")
            }
            PhotoManager.uploadCachedPhotos(newStockID, plantBinding, activity, lifecycleScope)
        } catch (e: Exception) {
            Log.e("PlantSaveManager", "Error uploading cached photos: ${e.message}", e)
            withContext(Dispatchers.Main) {
                showToast(activity, "Error uploading photos: ${e.message}")
            }
        }
    }

    private fun generateTempStockID(): Int {
        return -(1..Int.MAX_VALUE).random()
    }

    private fun showToast(activity: AppCompatActivity, message: String) {
        activity.runOnUiThread {
            android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
