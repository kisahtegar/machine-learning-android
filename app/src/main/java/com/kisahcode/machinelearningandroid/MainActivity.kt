package com.kisahcode.machinelearningandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.kisahcode.machinelearningandroid.CameraActivity.Companion.CAMERAX_RESULT
import com.kisahcode.machinelearningandroid.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * MainActivity is the main entry point of the application, allowing users to select images
 * from the gallery, capture images with the camera, analyze images using ML Kit, and view
 * the results.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null

    /**
     * Permission request launcher for camera permission.
     *
     * This launcher handles the result of the camera permission request. It displays a toast
     * message indicating whether the permission request was granted or denied.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                showToast("Permission request granted")
            } else {
                showToast("Permission request denied")
            }
        }

    /**
     * Checks if all required permissions are granted.
     *
     * This function checks whether the camera permission is granted to the application.
     * @return true if the camera permission is granted, false otherwise.
     */
    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
        binding.cameraXButton.setOnClickListener { startCameraX() }
        binding.analyzeButton.setOnClickListener {
            currentImageUri?.let {
                analyzeImage(it)
            } ?: run {
                showToast(getString(R.string.empty_image_warning))
            }
        }
    }

    /**
     * Starts the gallery to select an image.
     *
     * This function launches the system gallery app to allow the user to select an image.
     * The selected image will be used for further processing, such as uploading or analysis.
     */
    private fun startGallery() {
        // Launches the system gallery app with the option to select an image only
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    /**
     * Launcher for the gallery activity result.
     *
     * This variable registers an activity result launcher for picking visual media, such as images,
     * from the gallery. When an image is selected from the gallery, the launcher callback assigns
     * the URI of the selected image to the currentImageUri property. It then triggers the display
     * of the selected image. If no media is selected, a debug log message is printed.
     */
    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Assigns the URI of the selected image to the currentImageUri property
            currentImageUri = uri
            // Displays the selected image
            showImage()
        } else {
            // Prints a debug log message if no media is selected
            Log.d("Photo Picker", "No media selected")
        }
    }

    /**
     * Starts the camera to capture an image.
     *
     * This function initializes the process of capturing an image using the device's camera.
     * It retrieves the URI where the captured image will be saved and launches the camera intent.
     */
    private fun startCamera() {
        // Retrieves the URI where the captured image will be saved
        currentImageUri = getImageUri(this)
        // Launches the camera intent to capture an image
        launcherIntentCamera.launch(currentImageUri)
    }

    /**
     * Launcher for the camera activity result.
     *
     * This variable registers an activity result launcher for capturing images using the device's camera.
     * Upon successful image capture, it triggers the display of the captured image.
     */
    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        // Checks if the image capture operation was successful
        if (isSuccess) {
            // Displays the captured image
            showImage()
        }
    }

    /**
     * Starts the CameraX activity to capture an image.
     *
     * This function launches the CameraX activity to capture an image using the CameraX API.
     */
    private fun startCameraX() {
        // Creates an intent to start the CameraX activity
        val intent = Intent(this, CameraActivity::class.java)
        // Launches the CameraX activity
        launcherIntentCameraX.launch(intent)
    }

    /**
     * Launcher for the CameraX activity result.
     *
     * This variable registers an activity result launcher for handling the result of the CameraX activity.
     * It retrieves the URI of the captured image from the result data and triggers the display of the image.
     */
    private val launcherIntentCameraX = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Checks if the CameraX activity result code indicates a successful image capture
        if (it.resultCode == CAMERAX_RESULT) {
            // Retrieves the URI of the captured image from the result data
            currentImageUri = it.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
            // Displays the captured image
            showImage()
        }
    }

    /**
     * Displays the captured image.
     *
     * This function sets the captured image URI to the ImageView for preview.
     * If the URI is not null, it logs the URI and updates the ImageView with the captured image.
     */
    private fun showImage() {
        // Checks if the currentImageUri is not null
        currentImageUri?.let {
            // Logs the URI of the captured image
            Log.d("Image URI", "showImage: $it")
            // Sets the captured image URI to the ImageView for preview
            binding.previewImageView.setImageURI(it)
        }
    }

    /**
     * Analyzes the specified image URI using ML Kit Text Recognition, extracts text from the image,
     * and navigates to the ResultActivity to display the detected text.
     *
     * @param uri The URI of the image to be analyzed.
     */
    private fun analyzeImage(uri: Uri) {
        // Show progress indicator while processing the image
        binding.progressIndicator.visibility = View.VISIBLE

        // Initialize a TextRecognizer using default options
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Create an InputImage from the URI
        val inputImage: InputImage = InputImage.fromFilePath(this, uri)

        // Process the image using the TextRecognizer
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText: Text ->
                // Extract detected text from the VisionText
                val detectedText: String = visionText.text
                if (detectedText.isNotBlank()) {
                    // Hide progress indicator
                    binding.progressIndicator.visibility = View.GONE

                    // Create an intent to navigate to the ResultActivity and pass the image URI
                    val intent = Intent(this, ResultActivity::class.java)
                    intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString())
                    intent.putExtra(ResultActivity.EXTRA_RESULT, detectedText)
                    startActivity(intent)
                } else {
                    // Hide progress indicator and show a toast message if no text is recognized
                    binding.progressIndicator.visibility = View.GONE
                    showToast(getString(R.string.no_text_recognized))
                }
            }
            .addOnFailureListener {
                // Hide progress indicator and show a toast message if an error occurs during text recognition
                binding.progressIndicator.visibility = View.GONE
                showToast(it.message.toString())
            }
    }

    /**
     * Displays a toast message.
     *
     * This function shows a toast message with the provided message string.
     */
    private fun showToast(message: String) {
        // Shows a toast message with the provided message string
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Companion object containing the required camera permission.
     *
     * This object holds the permission string required for accessing the device camera.
     */
    companion object {
        // Required camera permission constant
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}