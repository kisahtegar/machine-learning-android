package com.kisahcode.machinelearningandroid

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.AudioData.AudioDataFormat
import com.google.mediapipe.tasks.components.containers.Classifications
import com.google.mediapipe.tasks.core.BaseOptions
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * A helper class for performing audio classification using MediaPipe.
 *
 * @property threshold The confidence threshold for classifying audio events.
 * @property maxResults The maximum number of classification results to return.
 * @property modelName The name of the TensorFlow Lite model file.
 * @property runningMode The mode in which the audio classifier runs (e.g., AUDIO_STREAM).
 * @property overlap The percentage of overlap between consecutive audio classification windows.
 * @property context The context used for loading the model file and managing resources.
 * @property classifierListener The listener interface for handling classification results and errors.
 */
class AudioClassifierHelper(
    val threshold: Float = 0.1f,
    val maxResults: Int = 3,
    val modelName: String = "yamnet.tflite",
    val runningMode: RunningMode = RunningMode.AUDIO_STREAM,
    val overlap: Float = 0.5f,
    val context: Context,
    var classifierListener: ClassifierListener? = null,
) {

    private var audioClassifier: AudioClassifier? = null
    private var recorder: AudioRecord? = null
    private var executor: ScheduledThreadPoolExecutor? = null

    /**
     * Initializes the audio classifier by setting up the required options and loading the model.
     */
    init {
        initClassifier()
    }

    /**
     * Sets up the audio classifier by creating an instance with the specified options.
     */
    private fun initClassifier() {
        try {
            // Build options for the audio classifier
            val optionsBuilder = AudioClassifier.AudioClassifierOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                .setRunningMode(runningMode)

            // Set up result and error listeners for streaming mode
            if (runningMode == RunningMode.AUDIO_STREAM) {
                optionsBuilder
                    .setResultListener(this::streamAudioResultListener)
                    .setErrorListener(this::streamAudioErrorListener)
            }

            // Build base options for the audio classifier
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath(modelName)
            optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

            // Create the audio classifier instance
            audioClassifier = AudioClassifier.createFromOptions(context, optionsBuilder.build())

            // Create an AudioRecord instance for streaming mode
            if (runningMode == RunningMode.AUDIO_STREAM) {
                recorder = audioClassifier?.createAudioRecord(
                    AudioFormat.CHANNEL_IN_DEFAULT,
                    SAMPLING_RATE_IN_HZ,
                    BUFFER_SIZE_IN_BYTES.toInt()
                )
            }
        } catch (e: IllegalStateException) {
            // Handle exceptions if the audio classifier fails to load
            classifierListener?.onError(context.getString(R.string.audio_classifier_failed))
            Log.e(TAG, "MP task failed to load with error: " + e.message)
        } catch (e: RuntimeException) {
            // Handle exceptions if the audio classifier fails to load
            classifierListener?.onError(context.getString(R.string.audio_classifier_failed))
            Log.e(TAG, "MP task failed to load with error: " + e.message)
        }
    }

    /**
     * Starts audio classification by initiating the audio recording and scheduling classification tasks.
     */
    fun startAudioClassification() {
        if (audioClassifier == null) {
            // Initialize the audio classifier if not already initialized
            initClassifier()
        }

        // Check if the recorder is already recording
        if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        // Start recording audio
        recorder?.startRecording()

        // Create a scheduled executor for periodic classification
        executor = ScheduledThreadPoolExecutor(1)

        // Define a classify runnable to classify audio at regular intervals
        val classifyRunnable = Runnable {
            recorder?.let { classifyAudioAsync(it) }
        }

        // Calculate the interval between classifications
        //
        // Each model will expect a specific audio recording length. This formula calculates that
        // length using the input buffer size and tensor format sample rate.
        // For example, YAMNET expects 0.975 second length recordings.
        // This needs to be in milliseconds to avoid the required Long value dropping decimals.
        val lengthInMilliSeconds = ((REQUIRE_INPUT_BUFFER_SIZE * 1.0f) / SAMPLING_RATE_IN_HZ) * 1000
        val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()

        // Schedule the classify runnable at fixed intervals
        executor?.scheduleAtFixedRate(
            classifyRunnable,
            0,
            interval,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * Performs asynchronous audio classification using the initialized audio classifier.
     *
     * @param audioRecord The audio recording data to be classified.
     */
    private fun classifyAudioAsync(audioRecord: AudioRecord) {
        // Create an AudioData object from the audioRecord
        val audioData = AudioData.create(
            AudioDataFormat.create(recorder?.format), SAMPLING_RATE_IN_HZ
        )
        audioData.load(audioRecord)

        // Measure the inference time
        val inferenceTime = SystemClock.uptimeMillis()

        // Perform asynchronous classification
        audioClassifier?.classifyAsync(audioData, inferenceTime)
    }

    /**
     * Stops audio classification by shutting down the classification executor and releasing resources.
     */
    fun stopAudioClassification() {
        executor?.shutdownNow()
        audioClassifier?.close()
        audioClassifier = null
        recorder?.stop()
    }

    /**
     * Handles audio classification results received in streaming mode.
     *
     * @param resultListener The audio classification result listener.
     */
    private fun streamAudioResultListener(resultListener: AudioClassifierResult) {
        // Extract classification results from the resultListener
        val classifications = resultListener.classificationResults().first().classifications()

        // Get the timestamp of the classification
        val timestamp = resultListener.timestampMs()

        // Pass the classification results and timestamp to the classifierListener
        classifierListener?.onResults(classifications, timestamp)
    }

    /**
     * Handles errors that occur during audio classification in streaming mode.
     *
     * @param e The runtime exception representing the error.
     */
    private fun streamAudioErrorListener(e: RuntimeException) {
        classifierListener?.onError(e.message.toString())
    }

    /**
     * An interface for handling classification results and errors.
     */
    interface ClassifierListener {
        /**
         * Called when an error occurs during audio classification.
         *
         * @param error The error message.
         */
        fun onError(error: String)

        /**
         * Called when audio classification results are available.
         *
         * @param results The list of classification results.
         * @param inferenceTime The time taken for inference.
         */
        fun onResults(
            results: List<Classifications>,
            inferenceTime: Long
        )
    }

    /**
     * Companion object holding constant values and utility functions for the AudioClassifierHelper class.
     */
    companion object {
        private const val TAG = "AudioClassifierHelper"

        private const val SAMPLING_RATE_IN_HZ = 16000
        private const val EXPECTED_INPUT_LENGTH = 0.975F
        private const val REQUIRE_INPUT_BUFFER_SIZE = SAMPLING_RATE_IN_HZ * EXPECTED_INPUT_LENGTH
        private const val BUFFER_SIZE_FACTOR: Int = 2
        private const val BUFFER_SIZE_IN_BYTES =
            REQUIRE_INPUT_BUFFER_SIZE * Float.SIZE_BYTES * BUFFER_SIZE_FACTOR
    }
}