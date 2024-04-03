package com.kisahcode.machinelearningandroid

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
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
 * @property context The context used for loading the model file.
 * @property modelName The name of the TensorFlow Lite model file.
 * @property onError A lambda function to handle prediction errors.
 * @property onResult A lambda function to handle the prediction result.
 */
class PredictionHelper(
    val context: Context,
    private val modelName: String = "rice_stock.tflite",
    private val onError: (String) -> Unit,
    private val onResult: (String) -> Unit,
) {

    private var interpreter: InterpreterApi? = null
    private var isGPUSupported: Boolean = false

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
                // Updates the flag to indicate GPU support.
                isGPUSupported = true
            }

            // Initializes TensorFlow Lite with the specified options.
            TfLite.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            // If TensorFlow Lite initialization succeeds, load the local model.
            loadLocalModel()
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

    /**
     * Initializes the TensorFlow Lite interpreter with the loaded model.
     *
     * @param model The TensorFlow Lite model to be used for inference. It can be provided as a ByteBuffer.
     */
    private fun initializeInterpreter(model: Any) {
        // Close the existing interpreter, if any, to release associated resources
        interpreter?.close()

        try {
            // Create options for configuring the TensorFlow Lite interpreter
            val options = InterpreterApi.Options()
                .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)

            // Add GPU delegate to interpreter options if GPU support is available
            if (isGPUSupported){
                options.addDelegateFactory(GpuDelegateFactory())
            }

            // Check if the model is provided as a ByteBuffer
            if (model is ByteBuffer) {
                // Create TensorFlow Lite interpreter using the provided model and options
                interpreter = InterpreterApi.create(model, options)
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