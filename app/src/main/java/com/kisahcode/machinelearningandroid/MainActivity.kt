package com.kisahcode.machinelearningandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.kisahcode.machinelearningandroid.databinding.ActivityMainBinding

/**
 * The main activity of the application responsible for user interaction and prediction.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPredict.isEnabled = false

        // Initialize PredictionHelper for performing predictions
        val predictionHelper = PredictionHelper(
            context = this,
            onResult = { result ->
                // Update UI with prediction result
                binding.tvResult.text = result
            },
            onError = { errorMessage ->
                // Display error message in a toast
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
            },
            onDownloadSuccess = {
                // Enable the predict button once the model is downloaded successfully
                binding.btnPredict.isEnabled = true
            }
        )

        // Set click listener for the predict button
        binding.btnPredict.setOnClickListener {
            // Get input from the EditText and perform prediction
            val input = binding.edSales.text.toString()
            predictionHelper.predict(input)
        }
    }
}