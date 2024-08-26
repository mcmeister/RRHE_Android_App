package com.example.rrhe

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.rrhe.databinding.ActivityPlantDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantDetailsBinding
    var stockID: Int = 0
    private var isTempID: Boolean = false // Track if this is a temporary ID
    private lateinit var editPlantActivityResultLauncher: ActivityResultLauncher<Intent>
    private var initialPhoto1: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editPlantActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                refreshPlantDetails()
            }
        }

        val plant = getPlantFromIntent(intent)
        if (plant != null) {
            val nonNullPlant = plant.ensureNonNullValues()
            stockID = nonNullPlant.StockID!! // Since StockID is never null
            isTempID = intent.getBooleanExtra("isTempID", false)
            initialPhoto1 = nonNullPlant.Photo1 // Capture the initial Photo1 value
            updatePlantDetails(nonNullPlant)
        } else {
            Log.e("PlantDetailsActivity", "Plant data is null")
            finish() // Close the activity if plant data is not available
        }

        binding.backButton.setOnClickListener {
            InactivityDetector(this).reset()
            finish()
        }

        binding.editButton.setOnClickListener {
            InactivityDetector(this).reset()
            fetchAndEditPlantDetails()
        }

        // Observe changes in the plant list
        PlantRepository.plants.observe(this) { plants ->
            val updatedPlant = plants.find { it.StockID == stockID }
            updatedPlant?.let { updatePlantDetails(it) }
        }

        // Enable OnBackInvokedCallback for API level 33 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        InactivityDetector(this).reset()

        // If this was a temporary ID, update it to the permanent ID if available
        if (isTempID) {
            lifecycleScope.launch(Dispatchers.IO) {
                val updatedPlant = PlantRepository.getPlantByStockID(stockID)
                updatedPlant?.let {
                    stockID = it.StockID!!
                    isTempID = false
                    withContext(Dispatchers.Main) {
                        updatePlantDetails(it)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        InactivityDetector(this).stop()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        InactivityDetector(this).reset()
        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleScope.launch {
            val currentPhoto1 = getCurrentPhoto1()

            // Clear cache only if Photo1 was changed or removed
            if (initialPhoto1 != currentPhoto1) {
                clearGlideCache()
                Log.d("PlantDetailsActivity", "Photo1 changed or removed, Glide cache cleared")
            } else {
                Log.d("PlantDetailsActivity", "Photo1 unchanged, Glide cache not cleared")
            }
        }
    }

    private fun getPlantFromIntent(intent: Intent?): Plant? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("plant", Plant::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("plant")
        }
    }

    private suspend fun getCurrentPhoto1(): String? {
        // Fetch the current Photo1 value from the plant repository or the current plant details in the activity
        val currentPlant = PlantRepository.getPlantByStockID(stockID)
        return currentPlant?.Photo1
    }

    private fun updatePlantDetails(plant: Plant) {
        with(binding) {
            // Helper function to set text and visibility using resource string with placeholders
            fun setTextView(textView: TextView, resId: Int, text: String?) {
                if (text.isNullOrEmpty()) {
                    textView.visibility = View.GONE
                } else {
                    textView.text = getString(resId, text)
                    textView.visibility = View.VISIBLE
                }
            }

            // Helper function to set text and visibility using resource string with placeholders for integers
            fun setTextViewInt(textView: TextView, resId: Int, value: Int?) {
                if (value == null) {
                    textView.visibility = View.GONE
                } else {
                    textView.text = getString(resId, value)
                    textView.visibility = View.VISIBLE
                }
            }

            // Helper function to set text and visibility using resource string with placeholders for integers, hiding if zero
            fun setTextViewIntZero(textView: TextView, resId: Int, value: Int?) {
                if (value == null || value == 0) {
                    textView.visibility = View.GONE
                } else {
                    textView.text = getString(resId, value)
                    textView.visibility = View.VISIBLE
                }
            }

            // Set text without label for plantName and plantThaiName
            fun setTextViewNoLabel(textView: TextView, text: String?) {
                if (text.isNullOrEmpty()) {
                    textView.visibility = View.GONE
                } else {
                    textView.text = text
                    textView.visibility = View.VISIBLE
                }
            }

            setTextViewNoLabel(plantName, plant.NameConcat)
            setTextViewNoLabel(plantThaiName, plant.ThaiName?.let { decodeUnicode(it) })
            setTextViewInt(plantStockId, R.string.stock_id_text, plant.StockID)
            setTextView(plantStatus, R.string.plant_status_text, plant.PlantStatus)
            setTextView(statusNote, R.string.status_note_text, plant.StatusNote)
            setTextView(plantDescription, R.string.plant_description_text, plant.PlantDescription)
            setTextViewIntZero(stockQty, R.string.stock_qty_text, plant.StockQty)
            setTextViewIntZero(stockPrice, R.string.stock_price_text, plant.StockPrice)
            setTextViewIntZero(usdPrice, R.string.usd_text, plant.USD)
            setTextViewIntZero(eurPrice, R.string.eur_text, plant.EUR)
            setTextViewIntZero(totalValue, R.string.total_value_text, plant.TotalValue)
            setTextView(plantMother, R.string.mother_text, if (plant.Mother == 1) "Yes" else "No")
            setTextView(plantWebsite, R.string.website_text, if (plant.Website == 1) "Yes" else "No")
            setTextView(tableName, R.string.table_name_text, plant.TableName)
            setTextView(traySize, R.string.tray_size_text, plant.TraySize)
            setTextViewIntZero(grams, R.string.grams_text, plant.Grams)

            // Set the Planted date range (No need to format if already strings)
            plantedTextView.text = getString(
                R.string.planted_text,
                plant.PlantedStart ?: "",
                plant.PlantedEnd ?: ""
            )

            // Update the date fields to reflect actual dates
            setTextView(pollinateDate, R.string.pollinate_date_text, plant.PollinateDate ?: "")
            setTextView(seedsPlanted, R.string.seeds_planted_text, plant.SeedsPlanted ?: "")
            setTextView(seedsHarvest, R.string.seeds_harvest_text, plant.SeedsHarvest ?: "")
            setTextView(lastEditedBy, R.string.last_edited_by_text, plant.LastEditedBy)

            // Conditionally show PurchasePrice, Mother Plant ID, and Father Plant ID based on PlantStatus
            when (plant.PlantStatus) {
                "Purchase" -> {
                    setTextViewIntZero(purchasePrice, R.string.purchase_price_text, plant.PurchasePrice)
                    plantMId.visibility = View.GONE
                    plantFId.visibility = View.GONE
                }
                "Propagate" -> {
                    setTextViewInt(plantMId, R.string.m_id_text, plant.M_ID)
                    plantFId.visibility = View.GONE
                    purchasePrice.visibility = View.GONE
                }
                "Pollinate" -> {
                    setTextViewInt(plantMId, R.string.m_id_text, plant.M_ID)
                    setTextViewInt(plantFId, R.string.f_id_text, plant.F_ID)
                    purchasePrice.visibility = View.GONE
                }
                else -> {
                    purchasePrice.visibility = View.GONE
                    plantMId.visibility = View.GONE
                    plantFId.visibility = View.GONE
                }
            }

            plantMId.setOnClickListener {
                navigateToPlantDetails(plant.M_ID)
            }

            plantFId.setOnClickListener {
                navigateToPlantDetails(plant.F_ID)
            }

            // Load the main plant image with improved error handling and logging
            loadImageWithRetry(plant.Photo1, plantImage)

            // Load photo links as image previews
            fun loadPhoto(imageView: ImageView, photo: String?) {
                if (photo.isNullOrEmpty()) {
                    imageView.visibility = View.GONE
                } else {
                    loadImageWithRetry(photo, imageView)
                    imageView.visibility = View.VISIBLE
                }
            }

            loadPhoto(photo2, plant.Photo2)
            loadPhoto(photo3, plant.Photo3)
            loadPhoto(photo4, plant.Photo4)

            // Display "Images:" text if any photo link is visible
            imagesLabel.visibility = if (photo2.visibility == View.VISIBLE || photo3.visibility == View.VISIBLE || photo4.visibility == View.VISIBLE) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Ensure photo links are displayed in one line
            photoContainer.visibility = if (photo2.visibility == View.VISIBLE || photo3.visibility == View.VISIBLE || photo4.visibility == View.VISIBLE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun loadImageWithRetry(photo: String?, imageView: ImageView) {
        if (isFinishing || isDestroyed) return  // Prevent loading if the activity is finishing or destroyed

        Glide.with(this@PlantDetailsActivity)
            .load(photo)
            .apply(RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache the image to prevent re-downloading
                .skipMemoryCache(true)  // Skip memory cache to avoid resource conflicts
                .placeholder(R.drawable.loading_placeholder)
                .error(R.drawable.error_image)
                .timeout(60000)  // Increase timeout to handle large images or slow networks
            )
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("Glide", "Failed to load image: $photo", e)
                    // Optionally, you can add a manual retry mechanism here
                    return false  // Let Glide handle the error display using the .error() option
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("Glide", "Image loaded successfully: $photo")
                    return false  // Allow the image to be set
                }
            })
            .into(imageView)
    }

    private fun clearGlideCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            Glide.get(applicationContext).clearDiskCache()
            withContext(Dispatchers.Main) {
                Glide.get(applicationContext).clearMemory()
            }
        }
    }

    fun refreshPlantDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedPlant = PlantRepository.getPlantByStockID(stockID)
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    updatedPlant?.let {
                        updatePlantDetails(it)
                    }
                }
            }
        }
    }

    private fun navigateToPlantDetails(stockID: Int?) {
        stockID?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val plant = PlantRepository.getPlantByStockID(it)
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        plant?.let {
                            val intent = Intent(this@PlantDetailsActivity, PlantDetailsActivity::class.java).apply {
                                putExtra("plant", plant)
                            }
                            InactivityDetector(this@PlantDetailsActivity).reset()
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    private fun fetchAndEditPlantDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            val plant = PlantRepository.getPlantByStockID(stockID)
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    plant?.let {
                        val intent = Intent(this@PlantDetailsActivity, EditPlantActivity::class.java).apply {
                            putExtra("plant", it)
                        }
                        editPlantActivityResultLauncher.launch(intent)
                    }
                }
            }
        }
    }
}
