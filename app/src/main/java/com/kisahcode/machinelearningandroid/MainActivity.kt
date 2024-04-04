package com.kisahcode.machinelearningandroid

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kisahcode.machinelearningandroid.databinding.ActivityMainBinding
import com.google.mediapipe.tasks.components.containers.Classifications
import java.text.NumberFormat

/**
 * The main activity of the application responsible for user interaction and text classification.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the TextClassifierHelper and set the classifier listener
        val textClassifierHelper = TextClassifierHelper(
            context = this,
            classifierListener = object : TextClassifierHelper.ClassifierListener {
                /**
                 * Called when an error occurs during text classification.
                 *
                 * @param error The error message.
                 */
                override fun onError(error: String) {
                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
                }

                /**
                 * Called when text classification results are available.
                 *
                 * @param results The list of classification results.
                 * @param inferenceTime The time taken for inference.
                 */
                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    runOnUiThread {
                        results?.let { it ->
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
                                // Set the classification result to the TextView
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

        // Set click listener for the classify button
        binding.btnClassify.setOnClickListener {
            // Get input text from EditText
            val inputText = binding.edInput.text.toString()
            // Perform text classification
            textClassifierHelper.classify(inputText)
        }
    }
}