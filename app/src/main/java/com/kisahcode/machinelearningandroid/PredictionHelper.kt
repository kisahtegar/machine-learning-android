package com.kisahcode.machinelearningandroid

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.GpuDelegateFactory
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * A helper class for performing predictions using a TensorFlow Lite model for rice stock prediction.
 *
 * This class facilitates the loading of a TensorFlow Lite model for predicting rice stock based on input data.
 * It provides methods to initialize the interpreter, perform predictions, and handle prediction outcomes.
 *
 * @property context The context used for loading the model file.
 * @property modelName The name of the TensorFlow Lite model file. Default value is "rice_stock.tflite".
 * @property onResult A lambda function to handle the prediction result. Invoked when a prediction is successfully made.
 * @property onError A lambda function to handle prediction errors. Invoked when an error occurs during prediction.
 * @property onDownloadSuccess A lambda function to handle successful model download. Invoked when the model is successfully downloaded.
 */
class PredictionHelper(
    private val modelName: String = "rice_stock.tflite",
    val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onDownloadSuccess: () -> Unit,
) {

    private var interpreter: InterpreterApi? = null

    /**
     * Initializes the PredictionHelper by checking GPU availability and loading the model.
     */
    init {
        // Checks if GPU delegate is available asynchronously.
        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable ->
            // Creates an options builder for TensorFlow Lite initialization.
            val optionsBuilder = TfLiteInitializationOptions.builder()

            // Checks if GPU delegate is available.
            if (gpuAvailable) {
                // Sets GPU delegate support if available.
                optionsBuilder.setEnableGpuDelegateSupport(true)
            }

            // Initializes TensorFlow Lite with the specified options.
            TfLite.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            downloadModel()
//            loadLocalModel()
        }.addOnFailureListener {
            // If TensorFlow Lite initialization fails, handle the error by calling the onError lambda function.
            onError(context.getString(R.string.tflite_is_not_initialized_yet))
        }
    }

    /**
     * Loads the local TensorFlow Lite model file from the assets folder.
     */
    private fun loadLocalModel() {
        try {
            // Loads the model file as a ByteBuffer from the assets folder.
            val buffer: ByteBuffer = loadModelFile(context.assets, modelName)
            // Initializes the interpreter with the loaded model buffer.
            initializeInterpreter(buffer)
        } catch (ioException: IOException) {
            // Handles any IOException that occurs during model loading by printing the stack trace.
            ioException.printStackTrace()
        }
    }

    @Synchronized
    private fun downloadModel(){
        // Set download conditions to require Wi-Fi
        val conditions = CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build()

        // Get the model from Firebase ML Model Downloader
        FirebaseModelDownloader.getInstance()
            .getModel("Rice-Stock", DownloadType.LOCAL_MODEL, conditions)
            .addOnSuccessListener { model: CustomModel ->
                try {
                    // Initialize interpreter with the downloaded model
                    initializeInterpreter(model)

                    // Invoke onDownloadSuccess lambda function
                    onDownloadSuccess()
                } catch (e: IOException) {
                    // If an I/O exception occurs, invoke onError lambda function with error message
                    onError(e.message.toString())
                }
            }
            .addOnFailureListener { e: Exception? ->
                // If download fails, invoke onError lambda function with appropriate error message
                onError(context.getString(R.string.firebaseml_model_download_failed))
            }
    }

    /**
     * Initializes the TensorFlow Lite interpreter with the loaded model.
     *
     * @param model The TensorFlow Lite model to be used for inference. It can be provided as a ByteBuffer
     * or CustomModel.
     */
    private fun initializeInterpreter(model: Any) {
        // Close the existing interpreter, if any, to release associated resources
        interpreter?.close()

        try {
            // Create options for configuring the TensorFlow Lite interpreter
            val options = InterpreterApi.Options()
                .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
                .addDelegateFactory(GpuDelegateFactory())

            // Check the type of the provided model and create interpreter accordingly
            if (model is ByteBuffer) {
                // Create interpreter directly from the ByteBuffer
                interpreter = InterpreterApi.create(model, options)
            } else if (model is CustomModel){
                // Create interpreter from the file associated with the CustomModel
                model.file?.let {
                    interpreter = InterpreterApi.create(it, options)
                }
            }
        } catch (e: Exception) {
            // Handle any exceptions that occur during interpreter initialization
            // Pass the error message to the onError callback function
            onError(e.message.toString())
            Log.e(TAG, e.message.toString())
        }
    }

    /**
     * Performs prediction using the TensorFlow Lite model.
     *
     * @param inputString The input value for prediction.
     */
    fun predict(inputString: String) {
        // Check if the TensorFlow Lite interpreter is initialized
        if (interpreter == null) {
            // If not initialized, return early
            return
        }

        // Prepare the input data for inference
        val inputArray = FloatArray(1)
        inputArray[0] = inputString.toFloat()

        // Initialize the output array to hold predicted values
        val outputArray = Array(1) { FloatArray(1) }

        try {
            // Run inference with the input data and store the predicted output
            interpreter?.run(inputArray, outputArray)
            // Pass the predicted output to the onResult callback function
            onResult(outputArray[0][0].toString())
        } catch (e: Exception) {
            // Handle any exceptions that occur during inference
            // Pass the error message to the onError callback function
            onError(context.getString(R.string.no_tflite_interpreter_loaded))
            Log.e(TAG, e.message.toString())
        }
    }

    /**
     * Loads the TensorFlow Lite model file from the assets folder.
     *
     * @param assetManager The AssetManager to access the assets folder.
     * @param modelPath The path to the TensorFlow Lite model file.
     * @return The MappedByteBuffer containing the model file.
     */
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        // Open the file descriptor for the model file
        assetManager.openFd(modelPath).use { fileDescriptor ->
            // Read the model file using FileInputStream
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                // Get the file channel from the input stream
                val fileChannel = inputStream.channel
                // Get the start offset and declared length of the model file
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength

                // Map the model file into memory as a MappedByteBuffer in read-only mode
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }

    /**
     * Closes the TensorFlow Lite interpreter.
     */
    fun close() {
        interpreter?.close()
    }

    companion object {
        private const val TAG = "PredictionHelper"
    }
}