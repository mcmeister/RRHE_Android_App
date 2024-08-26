@file:Suppress("DEPRECATION")

package com.example.rrhe

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.CaptureActivity

class QRScannerActivity : AppCompatActivity() {

    // Register activity result launcher for QR scan
    private val qrScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intentResult: IntentResult? = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        if (intentResult != null && intentResult.contents != null) {
            val stockId = intentResult.contents
            val resultIntent = Intent().apply {
                putExtra("scannedStockId", stockId)
            }
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initiateQrScan()
    }

    private fun initiateQrScan() {
        val integrator = IntentIntegrator(this).apply {
            setOrientationLocked(false)  // Ensure camera orientation is not locked
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)  // Set desired barcode format
            captureActivity = CustomCaptureActivity::class.java // Set custom capture activity for orientation
        }
        qrScanLauncher.launch(integrator.createScanIntent())
    }
}

class CustomCaptureActivity : CaptureActivity() {
    // This class can be used to set specific orientation or other camera configurations
}
