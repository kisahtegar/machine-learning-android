package com.kisahcode.machinelearningandroid

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.lifecycle.lifecycleScope
import com.kisahcode.machinelearningandroid.CameraActivity.Companion.CAMERAX_RESULT
import com.kisahcode.machinelearningandroid.data.api.ApiConfig
import com.kisahcode.machinelearningandroid.data.api.FileUploadResponse
import com.kisahcode.machinelearningandroid.databinding.ActivityMainBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

/**
 * This activity allows users to select images from the gallery, capture images using the device's camera,
 * or use the CameraX API for image capture. It also provides functionality to upload images for classification.
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
                // Display a toast message indicating that the permission request was granted
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_LONG).show()
            } else {
                // Display a toast message indicating that the permission request was denied
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
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

    /**
     * Initializes the main activity.
     *
     * This function inflates the layout, sets up UI components, and sets click listeners for buttons.
     * It also checks for camera permissions and requests them if not granted.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request camera permission if not granted
        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        // Set click listeners for buttons to perform respective actions
        binding.galleryButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
        binding.cameraXButton.setOnClickListener { startCameraX() }
        binding.uploadButton.setOnClickListener { uploadImage() }
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
    ) { result ->
        // Checks if the CameraX activity result code indicates a successful image capture
        if (result.resultCode == CAMERAX_RESULT) {
            // Retrieves the URI of the captured image from the result data
            currentImageUri = result.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
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
     * Uploads the captured image for classification.
     *
     * This function uploads the captured image to the server for classification using the API service.
     * It displays the classification result and handles error responses.
     */
    @SuppressLint("LongLogTag")
    private fun uploadImage() {
        // Checks if the currentImageUri is not null
        currentImageUri?.let { uri ->
            // Converts the URI to a file and reduces its size
            val imageFile = uriToFile(uri, this).reduceFileImage()
            // Logs the path of the uploaded image file
            Log.d("Image Classification File", "showImage: ${imageFile.path}")
            showLoading(true)

            // Creates a request body for the image file
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())

            // Creates a multipart form data with the image file
            val multipartBody = MultipartBody.Part.createFormData(
                "photo",
                imageFile.name,
                requestImageFile
            )

            // Executes the API call within the lifecycle scope
            lifecycleScope.launch {
                try {
                    // Retrieves the API service instance
                    val apiService = ApiConfig.getApiService()
                    // Uploads the image and receives the response
                    val successResponse = apiService.uploadImage(multipartBody)

                    // Handles the classification result response
                    with(successResponse.data){
                        // Displays the classification result in the resultTextView
                        binding.resultTextView.text = if (isAboveThreshold == true) {
                            showToast(successResponse.message.toString())
                            String.format("%s with %.2f%%", result, confidenceScore)
                        } else {
                            showToast("Model is predicted successfully but under threshold.")
                            String.format("Please use the correct picture because  the confidence score is %.2f%%", confidenceScore)
                        }
                    }
                    // Hides the loading progress indicator
                    showLoading(false)
                } catch (e: HttpException) {
                    // Handles HTTP error responses
                    val errorBody = e.response()?.errorBody()?.string()
                    val errorResponse = Gson().fromJson(errorBody, FileUploadResponse::class.java)

                    // Shows the error message in a toast
                    showToast(errorResponse.message.toString())
                    showLoading(false)
                }
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    /**
     * Displays or hides the loading progress indicator.
     *
     * This function sets the visibility of the progress indicator based on the isLoading parameter.
     * If isLoading is true, the progress indicator is set to visible; otherwise, it is set to gone.
     */
    private fun showLoading(isLoading: Boolean) {
        // Sets the visibility of the progress indicator based on the isLoading parameter
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
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