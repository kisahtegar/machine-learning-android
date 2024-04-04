package com.kisahcode.machinelearningandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kisahcode.machinelearningandroid.databinding.ActivityMainBinding
import com.google.mediapipe.tasks.components.containers.Classifications
import java.text.NumberFormat

/**
 * MainActivity class responsible for handling the main user interface and interaction.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioClassifierHelper: AudioClassifierHelper
    private var isRecording = false

    // Request permission launcher for handling permission requests
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val message = if (isGranted) "Permission granted" else "Permission denied"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeAudioClassifierHelper()
        setClickListener()
        updateButtonStates()
        requestPermissionsIfNeeded()
    }

    /**
     * Initializes the AudioClassifierHelper by creating an instance and setting up a listener
     * to handle classification results and errors.
     */
    private fun initializeAudioClassifierHelper() {
        audioClassifierHelper = AudioClassifierHelper(
            context = this,
            classifierListener = object : AudioClassifierHelper.ClassifierListener {

                /**
                 * Called when an error occurs during audio classification.
                 *
                 * @param error The error message.
                 */
                override fun onError(error: String) {
                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
                }

                /**
                 * Called when audio classification results are available.
                 *
                 * @param results The list of classification results.
                 * @param inferenceTime The time taken for inference.
                 */
                override fun onResults(results: List<Classifications>, inferenceTime: Long) {
                    // Update UI with classification results
                    runOnUiThread {
                        results.let { it ->
                            if (it.isNotEmpty() && it[0].categories().isNotEmpty()) {
                                println(it)

                                // Sort the categories by score in descending order
                                val sortedCategories =
                                    it[0].categories().sortedByDescending { it?.score() }
                                // Format the classification results for display
                                val displayResult =
                                    sortedCategories.joinToString("\n") {
                                        "${it.categoryName()} " + NumberFormat.getPercentInstance()
                                            .format(it.score()).trim()
                                    }
                                // Update the TextView with the formatted classification results
                                binding.tvResult.text = displayResult
                            } else {
                                // Clear the TextView if no results are available
                                binding.tvResult.text = ""
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Sets click listeners for start and stop buttons.
     */
    private fun setClickListener() {
        // Start button click listener
        binding.btnStart.setOnClickListener {
            audioClassifierHelper.startAudioClassification()
            isRecording = true
            updateButtonStates()
        }

        // Stop button click listener
        binding.btnStop.setOnClickListener {
            audioClassifierHelper.stopAudioClassification()
            isRecording = false
            updateButtonStates()
        }
    }

    /**
     * Updates the enabled state of start and stop buttons based on recording status.
     */
    private fun updateButtonStates() {
        binding.btnStart.isEnabled = !isRecording
        binding.btnStop.isEnabled = isRecording
    }

    /**
     * Called when the activity is resumed.
     * Resumes audio classification if recording was in progress before pausing the activity.
     */
    override fun onResume() {
        super.onResume()
        if (isRecording) {
            audioClassifierHelper.startAudioClassification()
        }
    }

    /**
     * Called when the activity is paused.
     * Stops audio classification when the activity is paused to release resources.
     */
    override fun onPause() {
        super.onPause()
        if (::audioClassifierHelper.isInitialized) {
            audioClassifierHelper.stopAudioClassification()
        }
    }

    /**
     * Requests necessary permissions for audio recording if not already granted.
     */
    private fun requestPermissionsIfNeeded() {
        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }
    }

    /**
     * Checks if all required permissions are granted.
     */
    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    // Companion object holding constant values
    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.RECORD_AUDIO
    }
}