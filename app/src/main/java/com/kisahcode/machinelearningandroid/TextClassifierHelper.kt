package com.kisahcode.machinelearningandroid

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.tasks.components.containers.Classifications
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import java.util.concurrent.ScheduledThreadPoolExecutor

/**
 * A helper class for performing text classification using TensorFlow Lite with MediaPipe.
 *
 * @property modelName The name of the TensorFlow Lite model file.
 * @property context The context used for loading the model file.
 * @property classifierListener The listener interface for handling classification results and errors.
 */
class TextClassifierHelper(
    val modelName: String = "bert_classifier.tflite",
    val context: Context,
    var classifierListener: ClassifierListener? = null,
) {

    private var textClassifier: TextClassifier? = null
    private var executor: ScheduledThreadPoolExecutor? = null

    /**
     * Initializes the text classifier by loading the TensorFlow Lite model.
     */
    init {
        initClassifier()
    }

    /**
     * Initializes the text classifier by loading the TensorFlow Lite model.
     * If the model loading fails, it notifies the listener about the error.
     */
    private fun initClassifier() {
        try {
            // Build options for the text classifier
            val optionsBuilder = TextClassifier.TextClassifierOptions.builder()

            // Build base options for the text classifier and set base options
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath(modelName)
            optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

            // Create a TextClassifier instance using the specified options
            textClassifier = TextClassifier.createFromOptions(context, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            // Handle the exception if text classifier creation fails
            // Notify the listener about the error
            classifierListener?.onError(context.getString(R.string.text_classifier_failed))
            Log.e(TAG, e.message.toString())
        }
    }

    /**
     * Classifies the input text using the initialized text classifier.
     *
     * @param inputText The input text to be classified.
     */
    fun classify(inputText: String) {
        if (textClassifier == null) {
            // Initialize the text classifier if not already initialized
            initClassifier()
        }

        // Create a new executor for the classification task
        executor = ScheduledThreadPoolExecutor(1)

        // Execute the classification task on the executor
        executor?.execute {
            // Measure the inference time
            var inferenceTime = SystemClock.uptimeMillis()
            // Perform classification on the input text
            val results = textClassifier?.classify(inputText)
            // Calculate the inference time
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            // Notify the listener about the classification results and inference time
            classifierListener?.onResults(results?.classificationResult()?.classifications(), inferenceTime)
        }
    }

    /**
     * An interface for handling classification results and errors.
     */
    interface ClassifierListener {

        /**
         * Called when an error occurs during text classification.
         *
         * @param error The error message.
         */
        fun onError(error: String)

        /**
         * Called when text classification results are available.
         *
         * @param results The list of classification results.
         * @param inferenceTime The time taken for inference.
         */
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }

    companion object {
        private const val TAG = "TextClassifierHelper"
    }
}