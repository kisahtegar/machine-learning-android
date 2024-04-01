package com.kisahcode.machinelearningandroid

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.kisahcode.machinelearningandroid.databinding.ActivityCameraBinding

/**
 * Activity responsible for capturing images using CameraX API.
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set onClickListeners for camera switch and capture buttons
        binding.switchCamera.setOnClickListener {
            cameraSelector =
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            startCamera()
        }
        binding.captureImage.setOnClickListener { takePhoto() }
    }

    /**
     * Called when the activity is resumed. Used to hide system UI and start the camera.
     */
    public override fun onResume() {
        super.onResume()
        hideSystemUI()
        startCamera()
    }

    private fun startCamera() {
        // Initialize the camera provider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Listen for the camera provider initialization completion
        cameraProviderFuture.addListener({
            // Get the initialized camera provider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configure and set up the preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Create an ImageCapture instance
            imageCapture = ImageCapture.Builder().build()

            try {
                // Unbind all previously bound use cases
                cameraProvider.unbindAll()

                // Bind the camera use cases to the activity's lifecycle
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector, // Select the desired camera (front or back)
                    preview,        // Provide the preview configuration
                    imageCapture    // Provide the image capture configuration
                )

            } catch (exc: Exception) {
                // Handle exceptions by displaying a toast message and logging the error
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
     * Captures a photo using the camera.
     *
     * This function captures a photo using the ImageCapture instance, saves it to a temporary file,
     * and handles the success or failure scenarios of capturing the image.
     */
    private fun takePhoto() {
        // Ensure that the ImageCapture instance is not null
        val imageCapture = imageCapture ?: return

        // Create a temporary file to save the captured photo
        val photoFile = createCustomTempFile(application)

        // Configure the output options for saving the captured photo
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Capture the photo with specified output options and handle success or failure scenarios
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                /**
                 * Invoked when the image is successfully saved.
                 *
                 * This method sends back the URI of the saved image via an intent and finishes the activity.
                 * @param output: Information about the saved image file.
                 */
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val intent = Intent()
                    intent.putExtra(EXTRA_CAMERAX_IMAGE, output.savedUri.toString())
                    setResult(CAMERAX_RESULT, intent)
                    finish()
                }

                /**
                 * Invoked when an error occurs while saving the image.
                 *
                 * This method displays a toast message indicating the failure and logs the error.
                 * @param exc: The exception representing the error.
                 */
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Gagal mengambil gambar.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "onError: ${exc.message}")
                }
            }
        )
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
     * Listens for changes in device orientation.
     *
     * This lazy-initialized property creates an OrientationEventListener to detect changes in device orientation.
     * It adjusts the target rotation of the ImageCapture instance accordingly to capture images correctly.
     */
    private val orientationEventListener by lazy {
        // Create an anonymous inner class extending OrientationEventListener
        object : OrientationEventListener(this) {

            /**
             * Invoked when device orientation changes.
             *
             * This method adjusts the target rotation of the ImageCapture instance based on the device orientation.
             * @param orientation: The new orientation angle in degrees.
             */
            override fun onOrientationChanged(orientation: Int) {
                // If the orientation is unknown, do nothing
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }
                // Determine the rotation based on the orientation angle
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                // Set the target rotation of the ImageCapture instance
                imageCapture?.targetRotation = rotation
            }
        }
    }

    /**
     * Called when the activity is becoming visible to the user.
     *
     * Registers the orientation event listener to enable orientation changes detection.
     */
    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    /**
     * Called when the activity is no longer visible to the user.
     *
     * Unregisters the orientation event listener to stop detecting orientation changes.
     */
    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
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