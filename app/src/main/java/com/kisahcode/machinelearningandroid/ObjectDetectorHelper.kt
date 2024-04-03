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
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import org.tensorflow.lite.task.gms.vision.classifier.Classifications
import org.tensorflow.lite.task.gms.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.gms.vision.detector.Detection
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector

/**
 * A helper class for performing object detection using TensorFlow Lite.
 *
 * @property threshold The confidence threshold for detecting objects.
 * @property maxResults The maximum number of detection results to return.
 * @property modelName The name of the TensorFlow Lite model file.
 * @property context The context used for loading the model file.
 * @property detectorListener The listener interface for handling detection results and errors.
 */
class ObjectDetectorHelper(
    var threshold: Float = 0.5f,
    var maxResults: Int = 5,
    val modelName: String = "efficientdet_lite0_v1.tflite",
    val context: Context,
    val detectorListener: DetectorListener?
) {
    private var objectDetector: ObjectDetector? = null

    /**
     * Initializes the object detector by setting up the required options and loading the model.
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
            // TensorFlow Lite Vision initialization success, proceed with setting up object detector
            setupObjectDetector()
        }.addOnFailureListener {
            // TensorFlow Lite Vision initialization failure, notify the listener
            detectorListener?.onError(context.getString(R.string.tflitevision_is_not_initialized_yet))
        }
    }

    /**
     * Sets up the object detector by creating an instance with the specified options.
     */
    private fun setupObjectDetector() {
        // Build options for the object detector
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        // Build base options for the object detector
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

        // Set the base options for the object detector
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            // Create an object detector instance from the model file and options
            objectDetector = ObjectDetector.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            // Handle the exception if object detector creation fails
            detectorListener?.onError(context.getString(R.string.object_detector_failed))
            Log.e(TAG, e.message.toString())
        }
    }

    /**
     * Detects objects in the image using the initialized object detector.
     *
     * @param image The image to be analyzed for object detection.
     */
    fun detectObject(image: ImageProxy) {
        // Check if TfLiteVision is initialized
        if (!TfLiteVision.isInitialized()) {
            val errorMessage = context.getString(R.string.tflitevision_is_not_initialized_yet)
            Log.e(TAG, errorMessage)
            detectorListener?.onError(errorMessage)
            return
        }

        // Ensure the object detector is initialized
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // Build an image processor to preprocess the input image
        val imageProcessor = ImageProcessor.Builder()
            .add(Rot90Op(-image.imageInfo.rotationDegrees / 90))
            .build()

        // Process the input image using the image processor
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(toBitmap(image)))

        // Measure the inference time
        var inferenceTime = SystemClock.uptimeMillis()

        // Detect objects in the processed image using the object detector
        val results = objectDetector?.detect(tensorImage)

        // Calculate the inference time
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // Notify the listener with the detection results and inference time
        detectorListener?.onResults(
            results,
            inferenceTime,
            tensorImage.height,
            tensorImage.width
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
     * An interface for handling object detection results and errors.
     */
    interface DetectorListener {

        /**
         * Called when an error occurs during object detection.
         *
         * @param error The error message.
         */
        fun onError(error: String)

        /**
         * Called when object detection results are available.
         *
         * @param results The list of detected objects.
         * @param inferenceTime The time taken for inference.
         * @param imageHeight The height of the input image.
         * @param imageWidth The width of the input image.
         */
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        private const val TAG = "ObjectDetectorHelper"
    }
}