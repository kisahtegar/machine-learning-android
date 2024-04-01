package com.kisahcode.machinelearningandroid

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants
private const val MAXIMAL_SIZE = 1000000 //1 MB
private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())

/**
 * getImageUri: Generates a URI for saving an image, considering API level.
 * This function creates a URI for saving the image either in the public gallery (for Android Q and above)
 * or in the app-specific directory (for devices running on pre-Q APIs).
 * @param context The application context.
 * @return The URI for saving the image.
 */
fun getImageUri(context: Context): Uri {
    var uri: Uri? = null
    // For devices running on Android Q and above, save the image in the public gallery
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Create content values for the image file
        val contentValues = ContentValues().apply {
            // Set the display name of the image file using a timestamp
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
            // Set the MIME type of the image file to JPEG
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            // Specify the relative path where the image file will be saved
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
        }

        // Insert the image file into the MediaStore using content resolver
        uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        // Example URI format: content://media/external/images/media/1000000062
        // Example file path: storage/emulated/0/Pictures/MyCamera/20230825_155303.jpg
    }

    // If the URI is null (for pre-Q devices), fallback to getImageUriForPreQ function
    return uri ?: getImageUriForPreQ(context)
}

/**
 * getImageUriForPreQ: Generates a URI for saving an image for devices running on pre-Q APIs.
 * This function creates a URI for saving the image in the app-specific directory for devices running
 * on Android versions prior to Android Q.
 * @param context The application context.
 * @return The URI for saving the image.
 */
private fun getImageUriForPreQ(context: Context): Uri {
    // Get the directory for saving images in the external files directory
    val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    // Create an image file with a timestamp as the filename
    val imageFile = File(filesDir, "/MyCamera/$timeStamp.jpg")

    // Create parent directories if they don't exist
    if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdir()

    // Generate a content URI for the image file using FileProvider
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        imageFile
    )
    // Example URI format: content://com.kisahcode.machinelearningandroid.fileprovider/my_images/MyCamera/20230825_133659.jpg
}

/**
 * createCustomTempFile: Creates a temporary image file.
 * This function creates a temporary image file in the application's external cache directory.
 * @param context The application context.
 * @return The temporary image file.
 */
fun createCustomTempFile(context: Context): File {
    // Get the directory for storing temporary files in the external cache directory
    val filesDir = context.externalCacheDir

    // Create a temporary image file with a timestamp as the filename and .jpg extension
    return File.createTempFile(timeStamp, ".jpg", filesDir)
}


/**
 * uriToFile: Converts URI to a file.
 * This function reads data from the provided URI and writes it to a file,
 * creating a new file in the application's external cache directory.
 * @param imageUri The URI of the image.
 * @param context The application context.
 * @return The image file.
 */
fun uriToFile(imageUri: Uri, context: Context): File {
    // Create a temporary file for storing the image data
    val myFile = createCustomTempFile(context)

    // Open an input stream to read data from the image URI
    val inputStream = context.contentResolver.openInputStream(imageUri) as InputStream

    // Create an output stream to write data to the temporary file
    val outputStream = FileOutputStream(myFile)
    val buffer = ByteArray(1024)
    var length: Int

    // Read data from the input stream and write it to the output stream
    while (inputStream.read(buffer).also { length = it } > 0) outputStream.write(buffer, 0, length)

    // Close the output stream and input stream
    outputStream.close()
    inputStream.close()

    // Return the temporary image file
    return myFile
}


/**
 * reduceFileImage: Reduces the size of the image file.
 * This function reads the image file, compresses it to reduce its size while maintaining quality,
 * and saves the compressed image back to the original file.
 * @return The reduced size image file.
 */
fun File.reduceFileImage(): File {
    // Get the reference to the image file
    val file = this

    // Decode the image file into a bitmap and rotate it if necessary
    val bitmap = BitmapFactory.decodeFile(file.path).getRotatedBitmap(file)
    var compressQuality = 100
    var streamLength: Int

    // Compress the bitmap until its size is below the maximum allowed size
    do {
        // Create a ByteArrayOutputStream to store the compressed bitmap
        val bmpStream = ByteArrayOutputStream()
        // Compress the bitmap with the current quality setting
        bitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
        // Convert the compressed bitmap to a byte array
        val bmpPicByteArray = bmpStream.toByteArray()
        // Get the length of the byte array (i.e., size of the compressed image)
        streamLength = bmpPicByteArray.size
        // Decrease the compression quality for the next iteration
        compressQuality -= 5
    } while (streamLength > MAXIMAL_SIZE)

    // Save the compressed bitmap back to the original file
    bitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, FileOutputStream(file))

    // Return the original file with reduced image size
    return file
}


/**
 * getRotatedBitmap: Rotates the bitmap based on Exif orientation.
 * This function reads the Exif orientation information from the image file and rotates
 * the bitmap accordingly to ensure the correct orientation.
 * @param file The image file.
 * @return The rotated bitmap.
 */
fun Bitmap.getRotatedBitmap(file: File): Bitmap? {
    // Get the Exif orientation of the image file
    val orientation = ExifInterface(file).getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED
    )
    // Rotate the bitmap based on the Exif orientation
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(this, 90F)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(this, 180F)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(this, 270F)
        ExifInterface.ORIENTATION_NORMAL -> this
        else -> this // Return the original bitmap if orientation is not specified or normal
    }
}

/**
 * rotateImage: Rotates the source bitmap by the given angle.
 * This function creates a new bitmap by rotating the source bitmap by the specified angle.
 * @param source The source bitmap.
 * @param angle The angle of rotation.
 * @return The rotated bitmap.
 */
fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
    // Create a matrix for rotation
    val matrix = Matrix()

    // Rotate the matrix by the specified angle
    matrix.postRotate(angle)

    // Create a new bitmap by applying the rotation matrix to the source bitmap
    return Bitmap.createBitmap(
        source, // Source bitmap
        0, 0, // Starting position (top-left corner)
        source.width, source.height, // Width and height of the source bitmap
        matrix, // Rotation matrix
        true // Filter to improve quality
    )
}