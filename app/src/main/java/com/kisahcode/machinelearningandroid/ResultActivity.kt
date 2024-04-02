package com.kisahcode.machinelearningandroid

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.kisahcode.machinelearningandroid.databinding.ActivityResultBinding

/**
 * ResultActivity displays the result of text detection on an image.
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    /**
     * Initializes the activity, sets up the UI components, and handles intent data.
     *
     * @param savedInstanceState The saved instance state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Display the image in the ImageView if URI is not null
        val imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI))
        imageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.resultImage.setImageURI(it)
        }

        // Retrieve the detected text from the intent and display it
        val detectedText = intent.getStringExtra(EXTRA_RESULT)
        binding.resultText.text = detectedText

        // Set up the click listener for the translate button
        binding.translateButton.setOnClickListener {
            // Show progress indicator when translating text
            binding.progressIndicator.visibility = View.VISIBLE
            // Translate the detected text
            translateText(detectedText)
        }
    }

    /**
     * Translates the detected text from English to Indonesian using ML Kit Translation API.
     *
     * @param detectedText The text detected by the ML model.
     */
    private fun translateText(detectedText: String?) {
        // Build translator options for translating from English to Indonesian
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.INDONESIAN)
            .build()

        // Create an English to Indonesian translator
        val indonesianEnglishTranslator = Translation.getClient(options)

        // Define conditions to download the translation model only when connected to Wi-Fi
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        // Download the translation model if needed
        indonesianEnglishTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // Translate the detected text from English to Indonesian
                indonesianEnglishTranslator.translate(detectedText.toString())
                    .addOnSuccessListener { translatedText ->
                        // Update UI with the translated text and hide progress indicator
                        binding.translatedText.text = translatedText
                        indonesianEnglishTranslator.close()
                        binding.progressIndicator.visibility = View.GONE
                    }
                    .addOnFailureListener { exception ->
                        // Handle translation failure and hide progress indicator
                        showToast(exception.message.toString())
                        print(exception.stackTrace)
                        indonesianEnglishTranslator.close()
                        binding.progressIndicator.visibility = View.GONE
                    }
            }
            .addOnFailureListener { exception ->
                // Handle model download failure and hide progress indicator
                showToast(getString(R.string.downloading_model_fail))
                binding.progressIndicator.visibility = View.GONE
            }

        // Add translator as a lifecycle observer to manage its lifecycle
        lifecycle.addObserver(indonesianEnglishTranslator)
    }

    /**
     * Displays a toast message with the specified message.
     *
     * @param message The message to be displayed in the toast.
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        // Extra keys for passing data to ResultActivity
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
    }
}