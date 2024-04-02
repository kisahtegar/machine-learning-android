package com.kisahcode.machinelearningandroid

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants
private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())

/**
 * Generates an image URI based on the device's API level.
 *
 * For devices running Android Q (API level 29) and above, the image is stored in the Pictures/MyCamera directory.
 * For devices running below Android Q, the image is stored in the external cache directory.
 *
 * @param context The application context.
 * @return The generated image URI.
 */
fun getImageUri(context: Context): Uri {
    var uri: Uri? = null

    // Check if the device is running on Android Q or above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Create ContentValues to store metadata about the image
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
        }

        // Insert the image into the MediaStore database
        uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        // The inserted URI of the image, e.g., content://media/external/images/media/1000000062
        // The actual file path of the image, e.g., storage/emulated/0/Pictures/MyCamera/20230825_155303.jpg
    }

    // If the URI is null (device not running on Android Q or above), fallback to the method for pre-Android Q devices
    return uri ?: getImageUriForPreQ(context)
}

/**
 * Generates an image URI for devices running below Android Q (API level 29).
 *
 * The image is stored in the external files directory.
 *
 * @param context The application context.
 * @return The generated image URI.
 */
private fun getImageUriForPreQ(context: Context): Uri {
    // Get the external files directory for storing images
    val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    // Create a File object representing the image file with timestamp as filename
    val imageFile = File(filesDir, "/MyCamera/$timeStamp.jpg")

    // Create the directory if it does not exist
    if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdir()

    // Generate a content URI for the image file using FileProvider
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        imageFile
    )

    // The generated content URI of the image file, e.g., content://com.dicoding.picodiploma.mycamera.fileprovider/my_images/MyCamera/20230825_133659.jpg
}

/**
 * Creates a custom temporary image file in the external cache directory.
 *
 * @param context The application context.
 * @return The created temporary image file.
 */
fun createCustomTempFile(context: Context): File {
    // Get the external cache directory for storing the temporary image file
    val filesDir = context.externalCacheDir

    // Create a temporary image file with a timestamp as the filename and ".jpg" extension
    return File.createTempFile(timeStamp, ".jpg", filesDir)
}