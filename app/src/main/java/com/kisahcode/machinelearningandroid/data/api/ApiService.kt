package com.kisahcode.machinelearningandroid.data.api

import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * ApiService interface defining the endpoints for interacting with the remote server.
 */
interface ApiService {

    /**
     * uploadImage, endpoint for uploading an image file to the server for classification.
     *
     * @param file The image file to be uploaded as a MultipartBody.Part.
     * @return FileUploadResponse containing the classification result for the uploaded image.
     */
    @Multipart
    @POST("skin-cancer/predict")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): FileUploadResponse
}