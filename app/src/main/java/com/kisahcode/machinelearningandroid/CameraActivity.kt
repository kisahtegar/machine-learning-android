package com.kisahcode.machinelearningandroid

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
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
import org.tensorflow.lite.task.gms.vision.classifier.Classifications
import org.tensorflow.lite.task.gms.vision.detector.Detection
import java.text.NumberFormat
import java.util.concurrent.Executors

/**
 * An activity to capture images and scan barcodes using the device's camera.
 */
class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

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
     * Starts the camera and sets up image analysis for object detection.
     */
    private fun startCamera() {
        // Initialize the ObjectDetectorHelper instance
        objectDetectorHelper = ObjectDetectorHelper(
            context = this,
            detectorListener = object : ObjectDetectorHelper.DetectorListener {

                /**
                 * Handles errors that occur during object detection.
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
                 * Processes the object detection results.
                 *
                 * @param results The list of detected objects.
                 * @param inferenceTime The time taken for inference.
                 * @param imageHeight The height of the input image.
                 * @param imageWidth The width of the input image.
                 */
                override fun onResults(
                    results: MutableList<Detection>?,
                    inferenceTime: Long,
                    imageHeight: Int,
                    imageWidth: Int
                ) {
                    runOnUiThread {
                        results?.let { it ->
                            if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                                println(it)

                                // Display the object detection results
                                val builder = StringBuilder()
                                for (result in results) {
                                    val displayResult =
                                        "${result.categories[0].label} " + NumberFormat.getPercentInstance()
                                            .format(result.categories[0].score).trim()
                                    builder.append("$displayResult \n")
                                }
                                binding.tvResult.text = builder.toString()
                                binding.tvResult.visibility = View.VISIBLE
                                binding.tvInferenceTime.text = "$inferenceTime ms"
                            } else {
                                // Clear UI if no object detection results are available
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
                // Perform object detection
                objectDetectorHelper.detectObject(image)
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