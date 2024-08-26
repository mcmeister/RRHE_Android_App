package com.example.rrhe

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.rrhe.databinding.ActivityEditPlantBinding
import com.example.rrhe.databinding.ActivityNewPlantBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object PhotoManager {

    private val tempFiles = mutableListOf<File>()

    // Interface to unify handling of both Edit and New Plant bindings
    interface PlantBinding {
        val photoEdit1: ImageView
        val photoEdit2: ImageView
        val photoEdit3: ImageView
        val photoEdit4: ImageView
        val saveButton: android.widget.Button
    }

    // Helper methods to create a PlantBinding interface from EditPlantBinding or NewPlantBinding
    fun getPlantBinding(binding: ActivityEditPlantBinding): PlantBinding {
        return object : PlantBinding {
            override val photoEdit1: ImageView = binding.photoEdit1
            override val photoEdit2: ImageView = binding.photoEdit2
            override val photoEdit3: ImageView = binding.photoEdit3
            override val photoEdit4: ImageView = binding.photoEdit4
            override val saveButton: android.widget.Button = binding.saveButton
        }
    }

    fun getPlantBinding(binding: ActivityNewPlantBinding): PlantBinding {
        return object : PlantBinding {
            override val photoEdit1: ImageView = binding.photoEdit1
            override val photoEdit2: ImageView = binding.photoEdit2
            override val photoEdit3: ImageView = binding.photoEdit3
            override val photoEdit4: ImageView = binding.photoEdit4
            override val saveButton: android.widget.Button = binding.saveButton
        }
    }

    fun handlePhotoChanged(
        photoPath: String,
        photoIndex: Int,
        currentPlant: Plant?,
        binding: PlantBinding,
        activity: AppCompatActivity,
        scope: CoroutineScope
    ) {
        val uri = Uri.parse(photoPath)
        currentPlant?.let {
            updateImageViewWithGlide(uri.toString(), binding, photoIndex, activity)

            scope.launch(Dispatchers.IO) {
                val resizedPhotoPath = resizePhoto(uri, activity)
                if (resizedPhotoPath != null) {
                    val resizedUri = Uri.fromFile(File(resizedPhotoPath))
                    withContext(Dispatchers.Main) {
                        updateImageViewWithGlide(resizedUri.toString(), binding, photoIndex, activity)
                    }
                    // For NewPlantActivity, do not upload immediately
                    if (activity is NewPlantActivity && currentPlant.StockID != null && currentPlant.StockID!! < 0) {
                        // Store in tempFiles for later upload
                        tempFiles.add(File(resizedPhotoPath))
                    } else {
                        handlePhotoUpload(resizedUri, activity, currentPlant.StockID ?: 0, photoIndex, binding, activity, scope)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast(activity, "Failed to resize and process the photo.")
                    }
                }
            }
        }
    }

    // Upload cached photos after StockID is updated
    fun uploadCachedPhotos(
        stockID: Int,
        binding: PlantBinding,
        activity: AppCompatActivity,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            tempFiles.forEachIndexed { index, file ->
                val photoUri = Uri.fromFile(file)
                val photoIndex = index + 1
                val photoPath = uploadPhotoToServer(photoUri, activity, stockID, photoIndex)
                withContext(Dispatchers.Main) {
                    if (photoPath != null) {
                        updateImageViewWithGlide(photoPath, binding, photoIndex, activity)
                        showToast(activity, "Photo $photoIndex uploaded successfully")
                    } else {
                        showToast(activity, "Failed to upload photo $photoIndex")
                    }
                }
            }
            tempFiles.clear()
        }
    }

    private fun resizePhoto(photoUri: Uri, context: Context): String? {
        Log.d("PhotoManager", "Attempting to resize photo from URI: $photoUri")

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val inputStream = try {
            context.contentResolver.openInputStream(photoUri)
        } catch (e: Exception) {
            Log.e("PhotoManager", "Error opening InputStream for URI: $photoUri", e)
            null
        }

        inputStream?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options)
        options.inJustDecodeBounds = false

        val bitmap = try {
            context.contentResolver.openInputStream(photoUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            Log.e("PhotoManager", "Error decoding bitmap from URI: $photoUri", e)
            null
        }

        if (bitmap == null) {
            Log.e("PhotoManager", "Failed to decode the photo from URI: $photoUri")
            return null
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 800, 600, true)
        val file = File(context.cacheDir, "resized_image_${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { outputStream ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
        }

        tempFiles.add(file)
        Log.d("PhotoManager", "Successfully resized photo and saved to: ${file.absolutePath}")
        return file.absolutePath
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val reqWidth = 800
        val reqHeight = 600
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun handlePhotoUpload(
        photoUri: Uri,
        context: Context,
        stockID: Int,
        photoIndex: Int,
        binding: PlantBinding,
        activity: AppCompatActivity,
        lifecycleScope: CoroutineScope
    ) {
        PlantSaveManager.isPhotoUploading = true
        activity.runOnUiThread {
            binding.saveButton.isEnabled = false
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val photoPath = uploadPhotoToServer(photoUri, context, stockID, photoIndex)

            withContext(Dispatchers.Main) {
                if (photoPath != null) {
                    updateImageViewWithGlide(photoPath, binding, photoIndex, activity)
                    showToast(activity, "Photo uploaded successfully")
                } else {
                    showToast(activity, "Failed to upload photo")
                }
                PlantSaveManager.isPhotoUploading = false
                binding.saveButton.isEnabled = true
            }
        }
    }

    internal suspend fun uploadPhotoToServer(photoUri: Uri, context: Context, stockID: Int, photoIndex: Int): String? {
        val fileName = "${stockID}_${photoIndex}.jpg"
        var tempFile: File?

        return withContext(Dispatchers.IO) {
            try {
                if (photoUri.scheme != ContentResolver.SCHEME_CONTENT && photoUri.scheme != ContentResolver.SCHEME_FILE) {
                    Log.e("PhotoManager", "Cannot upload photo: Not a local file URI")
                    return@withContext null
                }

                tempFile = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                    FileOutputStream(tempFile!!).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile!!.name, tempFile!!.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                    .build()

                val response = ApiClient.httpServerApiService.uploadPhoto(requestBody)

                if (response.isSuccessful) {
                    Log.d("PhotoManager", "Photo uploaded successfully: $fileName")
                    return@withContext ApiConfig.getHttpServerBaseUrl() + tempFile!!.name
                } else {
                    Log.e("PhotoManager", "Failed to upload photo")
                }
            } catch (e: Exception) {
                Log.e("PhotoManager", "Error uploading photo: ${e.message}", e)
            } finally {
                // Keep tempFile for later deletion after save or exit
            }
            return@withContext null
        }
    }

    fun handlePhotoRemoved(
        photoIndex: Int,
        currentPlant: Plant?,
        binding: Any, // Use Any to handle both binding types
        context: Context,
        scope: CoroutineScope
    ) {
        currentPlant?.let { plant ->
            when (photoIndex) {
                1 -> {
                    plant.Photo1 = null
                    when (binding) {
                        is ActivityEditPlantBinding -> {
                            clearImageView(binding.photoEdit1, context)
                        }

                        is ActivityNewPlantBinding -> {
                            clearImageView(binding.photoEdit1, context)
                        }

                        else -> {
                            throw IllegalArgumentException("Invalid binding type")
                        }
                    }
                }
                2 -> {
                    plant.Photo2 = null
                    when (binding) {
                        is ActivityEditPlantBinding -> {
                            clearImageView(binding.photoEdit2, context)
                        }

                        is ActivityNewPlantBinding -> {
                            clearImageView(binding.photoEdit2, context)
                        }

                        else -> {
                            throw IllegalArgumentException("Invalid binding type")
                        }
                    }
                }
                3 -> {
                    plant.Photo3 = null
                    when (binding) {
                        is ActivityEditPlantBinding -> {
                            clearImageView(binding.photoEdit3, context)
                        }

                        is ActivityNewPlantBinding -> {
                            clearImageView(binding.photoEdit3, context)
                        }

                        else -> {
                            throw IllegalArgumentException("Invalid binding type")
                        }
                    }
                }
                4 -> {
                    plant.Photo4 = null
                    when (binding) {
                        is ActivityEditPlantBinding -> {
                            clearImageView(binding.photoEdit4, context)
                        }

                        is ActivityNewPlantBinding -> {
                            clearImageView(binding.photoEdit4, context)
                        }

                        else -> {
                            throw IllegalArgumentException("Invalid binding type")
                        }
                    }
                }
            }
            plant.StockID?.let {
                deletePhoto(it, photoIndex, context, binding, scope) // Pass the binding and scope correctly
            }
            PlantUIUpdater.updateUI(binding, plant) // Correctly handles both binding types
        }
    }

    private fun clearImageView(imageView: ImageView, context: Context) {
        // Clear the Glide cache for the specific ImageView
        Glide.with(context)
            .clear(imageView)

        // Clear the memory and disk cache for this ImageView to ensure it doesn't reload the deleted image
        imageView.setImageDrawable(null)
        Glide.get(context).clearMemory() // Clear memory cache immediately
        CoroutineScope(Dispatchers.IO).launch {
            Glide.get(context).clearDiskCache() // Clear disk cache asynchronously
        }
    }

    private fun deletePhoto(
        stockID: Int,
        photoIndex: Int,
        context: Context,
        binding: Any, // Change to Any to handle both binding types
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.httpServerApiService.deletePhoto(
                    mapOf(
                        "stockID" to stockID.toString(),
                        "photoIndex" to photoIndex.toString()
                    )
                )
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        showToast(context, "Photo deleted successfully")
                        refreshPhotos(stockID, context, binding, scope) // Pass the correct binding and scope
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast(context, "Failed to delete photo")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast(context, "Error deleting photo: ${e.message}")
                }
            }
        }
    }

    fun deleteTempFiles() {
        for (file in tempFiles) {
            if (file.exists()) {
                file.delete()
            }
        }
        tempFiles.clear()
    }

    fun clearGlideCache(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Glide.get(context).clearDiskCache() // Clear disk cache asynchronously
        }
        Glide.get(context).clearMemory() // Clear memory cache immediately on the main thread
    }

    fun refreshPhotos(
        stockID: Int,
        context: Context,
        binding: Any, // Use Any to handle both binding types
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            val updatedPlant = PlantRepository.getPlantByStockID(stockID)
            withContext(Dispatchers.Main) {
                updatedPlant?.let {
                    clearGlideCache(context)
                    PlantUIUpdater.updateUI(binding, it) // Dynamically handles both binding types
                }
            }
        }
    }

    fun loadImageWithRetry(photo: String?, imageView: ImageView, context: Context) {
        val activity = context as? Activity
        if (activity?.isFinishing == true || activity?.isDestroyed == true) return

        if (photo.isNullOrEmpty()) {
            clearImageView(imageView, context)
            return
        }

        Glide.with(context)
            .load(photo)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    // Log the error
                    e?.logRootCauses("Glide Error")
                    // Implement retry logic here
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(imageView)
    }

    private fun updateImageViewWithGlide(photoPath: String, binding: PlantBinding, photoIndex: Int, context: Context) {
        when (photoIndex) {
            1 -> loadImageWithRetry(photoPath, binding.photoEdit1, context)
            2 -> loadImageWithRetry(photoPath, binding.photoEdit2, context)
            3 -> loadImageWithRetry(photoPath, binding.photoEdit3, context)
            4 -> loadImageWithRetry(photoPath, binding.photoEdit4, context)
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        cameraRequestCode: Int,
        activity: AppCompatActivity, // Updated this parameter to AppCompatActivity
        context: Context
    ) {
        if (requestCode == cameraRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                PlantListeners.takePhotoWithCamera(activity) // Pass the activity instead of binding
            } else {
                showToast(context, "Camera permission is required to take photos")
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
