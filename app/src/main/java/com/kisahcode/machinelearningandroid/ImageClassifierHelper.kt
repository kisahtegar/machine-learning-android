package com.kisahcode.machinelearningandroid

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Classifications
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier

/**
 * A helper class for performing image classification using TensorFlow Lite and MediaPipe.
 *
 * @property threshold The confidence threshold for classifying objects.
 * @property maxResults The maximum number of classification results to return.
 * @property modelName The name of the TensorFlow Lite model file.
 * @property runningMode The running mode for the image classifier.
 * @property context The context used for loading the model file.
 * @property classifierListener The listener interface for handling classification results and errors.
 */
class ImageClassifierHelper(
    var threshold: Float = 0.1f,
    var maxResults: Int = 3,
    val modelName: String = "mobilenet_v1.tflite",
    val runningMode: RunningMode = RunningMode.LIVE_STREAM,
    val context: Context,
    val classifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    /**
     * Initializes the image classifier by setting up the required options and loading the model.
     */
    init {
        setupImageClassifier()
    }

    /**
     * Sets up the image classifier by creating an instance with the specified options.
     * This method configures the image classifier with the provided threshold, maximum results,
     * and running mode. It also sets up listeners for handling classification results and errors.
     */
    private fun setupImageClassifier() {
        // Build options for the image classifier
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)
            .setRunningMode(runningMode)

        // If the running mode is LIVE_STREAM, set up result and error listeners
        if (runningMode == RunningMode.LIVE_STREAM) {
            optionsBuilder.setResultListener { result, image ->
                // Calculate inference time
                val finishTimeMs = SystemClock.uptimeMillis()
                val inferenceTime = finishTimeMs - result.timestampMs()

                // Notify the classifier listener about the classification results
                classifierListener?.onResults(
                    result.classificationResult().classifications(),
                    inferenceTime
                )
            }.setErrorListener { error ->
                // Notify the classifier listener about any errors
                classifierListener?.onError(error.message.toString())
            }
        }

        // Build base options for the image classifier
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath(modelName)

        // Set the base options for the image classifier
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            // Create an instance of the image classifier with the configured options
            imageClassifier = ImageClassifier.createFromOptions(
                context,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            // Handle the exception if image classifier creation fails
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
            Log.e(TAG, e.message.toString())
        }
    }

    /**
     * Classifies the image using the initialized image classifier.
     * This method performs classification on the provided image and notifies the classifier listener
     * about the classification results and inference time.
     *
     * @param image The image to be classified in ImageProxy format.
     */
    fun classifyImage(image: ImageProxy) {
        // Ensure the image classifier is initialized
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Convert the ImageProxy to a MediaPipe Image
        val mpImage = BitmapImageBuilder(toBitmap(image)).build()

        // Set up image processing options
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(image.imageInfo.rotationDegrees)
            .build()

        // Measure the inference time
        val inferenceTime = SystemClock.uptimeMillis()

        // Perform asynchronous classification on the image
        imageClassifier?.classifyAsync(mpImage, imageProcessingOptions, inferenceTime)
    }

    /**
     * Converts the image from ImageProxy format to Bitmap format.
     *
     * @param image The image in ImageProxy format to be converted.
     * @return The converted image in Bitmap format.
     */
    private fun toBitmap(image: ImageProxy): Bitmap {
        // Create a Bitmap buffer with the same dimensions as the image
        val bitmapBuffer = Bitmap.createBitmap(
            image.width,
            image.height,
            Bitmap.Config.ARGB_8888
        )

        // Copy pixel data from the image planes to the Bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        // Close the image proxy
        image.close()

        // Return the converted Bitmap
        return bitmapBuffer
    }

    /**
     * An interface for handling classification results and errors.
     */
    interface ClassifierListener {

        /**
         * Called when an error occurs during classification.
         *
         * @param error The error message.
         */
        fun onError(error: String)

        /**
         * Called when classification results are available.
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
        private const val TAG = "ImageClassifierHelper"
    }
}