package com.kisahcode.machinelearningandroid

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.kisahcode.machinelearningandroid.databinding.ActivityCameraBinding
import com.google.mediapipe.tasks.components.containers.Classifications
import java.text.NumberFormat
import java.util.concurrent.Executors

/**
 * An activity to capture images and scan barcodes using the device's camera.
 */
class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var imageClassifierHelper: ImageClassifierHelper

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
     * Starts the camera and sets up image analysis for classification.
     */
    private fun startCamera() {
        // Initialize the ImageClassifierHelper instance
        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {

                /**
                 * Handles errors that occur during classification.
                 *
                 * @param error The error message.
                 */
                override fun onError(error: String) {
                    // Show error message in UI
                    runOnUiThread {
                        Toast.makeText(this@CameraActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }

                /**
                 * Processes the classification results.
                 *
                 * @param results The list of classification results.
                 * @param inferenceTime The time taken for inference.
                 */
                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    runOnUiThread {
                        results?.let { it ->
                            if (it.isNotEmpty() && it[0].categories().isNotEmpty()) {
                                println(it)
                                // Sort categories by score and display results in UI
                                val sortedCategories =
                                    it[0].categories().sortedByDescending { it?.score() }
                                val displayResult =
                                    sortedCategories.joinToString("\n") {
                                        "${it.categoryName()} " + NumberFormat.getPercentInstance()
                                            .format(it.score()).trim()
                                    }

                                // Update UI with classification results and inference time
                                binding.tvResult.text = displayResult
                                binding.tvInferenceTime.text = "$inferenceTime ms"
                            } else {
                                // Clear UI if no classification results are available
                                binding.tvResult.text = ""
                                binding.tvInferenceTime.text = ""
                            }
                        }
                    }
                }
            }
        )

        // Initialize the camera provider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Set up camera provider listener
        cameraProviderFuture.addListener({
            // Configure the image analyzer
            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                // Perform image classification
                imageClassifierHelper.classifyImage(image)
            }

            // Get the camera provider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Set up preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // Bind camera to lifecycle
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                // Handle camera setup failure
                Toast.makeText(
                    this@CameraActivity,
                    "Gagal memunculkan kamera.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "startCamera: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
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