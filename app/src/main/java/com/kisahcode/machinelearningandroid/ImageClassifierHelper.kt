package com.kisahcode.machinelearningandroid

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageProxy
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import org.tensorflow.lite.task.gms.vision.classifier.Classifications
import org.tensorflow.lite.task.gms.vision.classifier.ImageClassifier

/**
 * A helper class for performing image classification using TensorFlow Lite.
 *
 * @property threshold The confidence threshold for classifying objects.
 * @property maxResults The maximum number of classification results to return.
 * @property modelName The name of the TensorFlow Lite model file.
 * @property context The context used for loading the model file.
 * @property classifierListener The listener interface for handling classification results and errors.
 */
class ImageClassifierHelper(
    var threshold: Float = 0.1f,
    var maxResults: Int = 3,
    val modelName: String = "mobilenet_v1.tflite",
    val context: Context,
    val classifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    /**
     * Initializes the image classifier by setting up the required options and loading the model.
     */
    init {
        // Check if GPU delegate is available
        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable ->
            // Initialize TensorFlow Lite Vision with appropriate options
            val optionsBuilder = TfLiteInitializationOptions.builder()
            if (gpuAvailable) {
                // Enable GPU delegate support if available
                optionsBuilder.setEnableGpuDelegateSupport(true)
            }
            // Initialize TensorFlow Lite Vision with the chosen options
            TfLiteVision.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            // TensorFlow Lite Vision initialization success, proceed with setting up image classifier
            setupImageClassifier()
        }.addOnFailureListener {
            // TensorFlow Lite Vision initialization failure, notify the listener
            classifierListener?.onError(context.getString(R.string.tflitevision_is_not_initialized_yet))
        }
    }

    /**
     * Sets up the image classifier by creating an instance with the specified options.
     */
    private fun setupImageClassifier() {
        // Build options for the image classifier
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        // Build base options for the image classifier
        val baseOptionsBuilder = BaseOptions.builder()

        // Check device compatibility for hardware acceleration options
        if (CompatibilityList().isDelegateSupportedOnThisDevice){
            // Use GPU delegate if supported
            baseOptionsBuilder.useGpu()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
            // Use NNAPI delegate for devices running Android Oreo MR1 (API 27) or higher
            baseOptionsBuilder.useNnapi()
        } else {
            // Fall back to CPU if hardware acceleration is not available
            baseOptionsBuilder.setNumThreads(4)
        }

        // Set the base options for the image classifier
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            // Create an image classifier instance from the model file and options
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
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
     *
     * @param image The image to be classified.
     */
    fun classifyImage(image: ImageProxy) {
        // Check if TfLiteVision is initialized
        if (!TfLiteVision.isInitialized()) {
            val errorMessage = context.getString(R.string.tflitevision_is_not_initialized_yet)
            Log.e(TAG, errorMessage)
            classifierListener?.onError(errorMessage)
            return
        }

        // Ensure the image classifier is initialized
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Build an image processor to resize and cast the input image
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(DataType.UINT8))
            .build()

        // Process the input image using the image processor
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(toBitmap(image)))

        // Build image processing options based on the image orientation
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(image.imageInfo.rotationDegrees))
            .build()

        // Measure the inference time
        var inferenceTime = SystemClock.uptimeMillis()

        // Classify the processed image using the image classifier
        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)

        // Calculate the inference time
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // Notify the listener with the classification results and inference time
        classifierListener?.onResults(
            results,
            inferenceTime
        )
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
     * Determines the image orientation based on the rotation angle.
     *
     * @param rotation The rotation angle of the image.
     * @return The corresponding image orientation.
     */
    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
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