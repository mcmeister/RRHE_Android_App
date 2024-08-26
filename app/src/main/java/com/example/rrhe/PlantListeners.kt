package com.example.rrhe

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PlantListeners {

    private lateinit var selectPhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private var currentPhotoIndex: Int = 0
    private var currentPhotoFilePath: String? = null
    private const val CAMERA_REQUEST_CODE = 101

    // Text Change Listeners
    fun setupTextChangeListeners(
        familyAutoCompleteTextView: AutoCompleteTextView,
        speciesAutoCompleteTextView: AutoCompleteTextView,
        subspeciesAutoCompleteTextView: AutoCompleteTextView,
        stockPriceEditText: EditText,
        stockQtyEditText: EditText,
        textChangeListener: (String) -> Unit
    ) {
        stockPriceEditText.addTextChangedListener { textChangeListener(it.toString()) }
        stockQtyEditText.addTextChangedListener { textChangeListener(it.toString()) }
        familyAutoCompleteTextView.addTextChangedListener { textChangeListener(it.toString()) }
        speciesAutoCompleteTextView.addTextChangedListener { textChangeListener(it.toString()) }
        subspeciesAutoCompleteTextView.addTextChangedListener { textChangeListener(it.toString()) }
    }

    // Dropdown Listeners
    fun setupDropdownListeners(
        familyAutoCompleteTextView: AutoCompleteTextView,
        speciesAutoCompleteTextView: AutoCompleteTextView,
        updateSpeciesDropdown: (String) -> Unit,
        updateSubspeciesDropdown: (String, String) -> Unit
    ) {
        familyAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedFamily = parent.getItemAtPosition(position).toString()
            updateSpeciesDropdown(selectedFamily)
        }

        speciesAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedSpecies = parent.getItemAtPosition(position).toString()
            val selectedFamily = familyAutoCompleteTextView.text.toString()
            updateSubspeciesDropdown(selectedFamily, selectedSpecies)
        }
    }

    // Save Button Listener for Editing Existing Plant
    fun setupSaveButtonListener(
        saveButton: Button,
        plant: Plant?,
        savePlantLocallyAndSync: (Plant) -> Unit
    ) {
        saveButton.setOnClickListener {
            plant?.let { originalPlant ->
                savePlantLocallyAndSync(originalPlant)
            }
        }
    }

    // Photo Click Listeners
    fun setupPhotoClickListeners(
        photoViews: List<ImageView>,
        photoPaths: List<String?>,
        plant: Plant?,
        onPhotoChanged: suspend (String, Int) -> Unit,
        onPhotoRemoved: (Int) -> Unit,
        activity: AppCompatActivity
    ) {
        selectPhotoLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data?.data?.let { photoUri ->
                    activity.lifecycleScope.launch {
                        onPhotoChanged(photoUri.toString(), currentPhotoIndex)
                    }
                }
            }
        }

        takePhotoLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                currentPhotoFilePath?.let { path ->
                    val photoUri = FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.fileprovider",
                        File(path)
                    )
                    activity.lifecycleScope.launch {
                        onPhotoChanged(photoUri.toString(), currentPhotoIndex)
                    }
                }
            }
        }

        photoViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                currentPhotoIndex = index + 1
                val photoPath = photoPaths[index]
                if (photoPath.isNullOrEmpty()) {
                    showActionChooser(activity)
                } else {
                    plant?.let { plantData ->
                        showFullScreenPhoto(
                            photoPath,
                            onOptionsClicked = {
                                showPhotoOptions(currentPhotoIndex, onPhotoRemoved, activity)
                            },
                            stockID = plantData.StockID.toString(),
                            photoIndex = currentPhotoIndex,
                            activity = activity
                        )
                    }
                }
            }
        }
    }

    private fun showFullScreenPhoto(
        photoPath: String,
        onOptionsClicked: () -> Unit,
        stockID: String,
        photoIndex: Int,
        activity: AppCompatActivity
    ) {
        val builder = AlertDialog.Builder(activity)
        val imageView = ImageView(activity)

        Glide.with(activity)
            .load(photoPath)
            .error(R.drawable.error_image)
            .into(imageView)

        imageView.apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        builder.setView(imageView)
            .setPositiveButton("Options") { _, _ ->
                onOptionsClicked()
            }
            .setNegativeButton("Download") { _, _ ->
                activity.lifecycleScope.launch {
                    downloadPhoto(photoPath, stockID, photoIndex, activity)
                }
            }
            .show()
    }

    private suspend fun downloadPhoto(photoPath: String, stockID: String, photoIndex: Int, activity: AppCompatActivity) {
        val context = activity.applicationContext

        val glideFile = withContext(Dispatchers.IO) {
            Glide.with(context)
                .asFile()
                .load(photoPath)
                .submit()
                .get()
        }

        val fileName = "${stockID}_${photoIndex}.jpg"

        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeFormatted = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(currentTimeMillis))

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DATE_ADDED, currentTimeMillis / 1000L)
            put(MediaStore.Images.Media.DATE_MODIFIED, currentTimeMillis / 1000L)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(it).use { outputStream ->
                        glideFile.inputStream().copyTo(outputStream!!)
                    }
                }

                withContext(Dispatchers.IO) {
                    val photoFile = File(glideFile.path)
                    val exif = ExifInterface(photoFile)
                    exif.setAttribute(ExifInterface.TAG_DATETIME, currentTimeFormatted)
                    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, currentTimeFormatted)
                    exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, currentTimeFormatted)
                    exif.saveAttributes()
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Log.d("PlantListeners", "Photo saved to gallery: $uri")
                showToast(context, "Photo saved to gallery")

                MediaScannerConnection.scanFile(context, arrayOf(uri.toString()), null) { _, _ ->
                    Log.d("PlantListeners", "MediaScanner completed.")
                }

            } catch (e: IOException) {
                Log.e("PlantListeners", "Failed to save photo to gallery", e)
            }
        } ?: run {
            Log.e("PlantListeners", "Failed to create MediaStore entry")
        }
    }

    private fun showPhotoOptions(
        photoIndex: Int,
        onPhotoRemoved: (Int) -> Unit,
        activity: AppCompatActivity
    ) {
        val options = arrayOf("Remove Photo", "Replace Photo")
        val builder = AlertDialog.Builder(activity)
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> onPhotoRemoved(photoIndex)
                1 -> showActionChooser(activity)
            }
        }
        builder.show()
    }

    private fun showActionChooser(activity: AppCompatActivity) {
        val options = arrayOf("Upload from Phone", "Take Photo")
        val builder = AlertDialog.Builder(activity)
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> selectPhotoFromGallery()
                1 -> checkCameraPermission(activity)
            }
        }
        builder.show()
    }

    private fun selectPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectPhotoLauncher.launch(intent)
    }

    private fun checkCameraPermission(activity: AppCompatActivity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            takePhotoWithCamera(activity)
        }
    }

    internal fun takePhotoWithCamera(activity: AppCompatActivity) {
        val photoFile = createImageFile(activity) ?: return
        currentPhotoFilePath = photoFile.absolutePath
        val photoUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        takePhotoLauncher.launch(intent)
    }

    private fun createImageFile(activity: AppCompatActivity): File? {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (ex: IOException) {
            Log.e("PlantListeners", "Error occurred while creating the file", ex)
            null
        }
    }
}
