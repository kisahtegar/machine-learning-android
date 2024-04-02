package com.kisahcode.machinelearningandroid

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.kisahcode.machinelearningandroid.databinding.ActivityCameraBinding

/**
 * An activity to capture images and scan barcodes using the device's camera.
 */
class CameraActivity : AppCompatActivity() {
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var binding: ActivityCameraBinding
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var firstCall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    /**
     * Called when the activity is resumed. Used to hide system UI and start the camera.
     */
    public override fun onResume() {
        super.onResume()
        hideSystemUI()
        startCamera()
    }

    /**
     * Starts the camera and sets up barcode scanning.
     */
    private fun startCamera() {
        // Configure barcode scanner options
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        // Initialize the barcode scanner
        barcodeScanner = BarcodeScanning.getClient(options)

        // Create an analyzer for barcode scanning
        val analyzer = MlKitAnalyzer(
            listOf(barcodeScanner),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            ContextCompat.getMainExecutor(this)
        ) { result: MlKitAnalyzer.Result? ->
            // Process the result of barcode scanning
            showResult(result)
        }

        // Initialize the camera controller
        val cameraController = LifecycleCameraController(baseContext)

        // Set the analyzer for image analysis
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            analyzer
        )

        // Bind camera controller to activity's lifecycle
        cameraController.bindToLifecycle(this)

        // Set the camera controller for the view finder
        binding.viewFinder.controller = cameraController
    }

    /**
     * Displays the result of barcode scanning in an alert dialog.
     *
     * @param result The result of the barcode scanning process.
     */
    private fun showResult(result: MlKitAnalyzer.Result?) {
        // Check if it is the first call to show the result
        if (firstCall) {
            val barcodeResults = result?.getValue(barcodeScanner)

            // Check if barcode results are not null and contain valid data
            if ((barcodeResults != null) &&
                (barcodeResults.size != 0) &&
                (barcodeResults.first() != null)
            ) {
                firstCall = false
                val barcode = barcodeResults[0]

                // Create an alert dialog to display the barcode raw value
                val alertDialog = AlertDialog.Builder(this)
                alertDialog
                    .setMessage(barcode.rawValue)
                    .setPositiveButton( "Buka" ) { _, _ ->
                        firstCall = true

                        // Handle different types of barcode values
                        when (barcode.valueType) {
                            Barcode.TYPE_URL -> {
                                // If the barcode is a URL, open it in a web browser
                                val openBrowserIntent = Intent(Intent.ACTION_VIEW)
                                openBrowserIntent.data = Uri.parse(barcode.url?.url)
                                startActivity(openBrowserIntent)
                            }

                            else -> {
                                // If the barcode value type is not supported, show a toast message
                                Toast.makeText(this, "Unsupported data type", Toast.LENGTH_SHORT)
                                    .show()

                                // Restart the camera for scanning again
                                startCamera()
                            }
                        }
                    }
                    .setNegativeButton("Scan lagi") { _, _ ->
                        // Allow scanning again
                        firstCall = true
                    }
                    .setCancelable(false)
                    .create()

                alertDialog.show()
            }
        }
    }

    /**
     * Hides the system UI elements such as status bar and action bar.
     *
     * This function hides the system UI elements using the appropriate methods based on the Android version.
     */
    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        // Check if the device is running Android version R or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use the WindowInsets API to hide the status bars
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            // For older Android versions, use flags to set the window to fullscreen mode
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        // Hide the action bar
        supportActionBar?.hide()
    }

    /**
     * Companion object holding constants and shared properties for CameraActivity.
     */
    companion object {
        private const val TAG = "CameraActivity"
        // Extra key for passing captured image URI between activities
        const val EXTRA_CAMERAX_IMAGE = "CameraX Image"
        // Result code for camera operations
        const val CAMERAX_RESULT = 200
    }
}