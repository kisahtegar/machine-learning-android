package com.kisahcode.machinelearningandroid.data.api

import com.google.gson.annotations.SerializedName

/**
 * FileUploadResponse, data class represents the response received from the server
 * after uploading a file for image classification.
 *
 * @param message A message from the server indicating the status of the request.
 * @param data Contains additional information including the classification result.
 */
data class FileUploadResponse(
    @SerializedName("message")
    var message: String? = null,
    @SerializedName("data")
    var data: Data = Data()
)

/**
 * Data, data class holds the detailed information returned by the server
 * regarding the uploaded file's classification result.
 *
 * @param id The unique identifier assigned to the uploaded file by the server.
 * @param result The classification result indicating the predicted label for the image.
 * @param confidenceScore The confidence score associated with the classification result.
 * @param isAboveThreshold A boolean flag indicating whether the confidence score is above a certain threshold.
 * @param createdAt The timestamp indicating when the classification result was generated.
 */
data class Data(
    @SerializedName("id")
    var id: String? = null,
    @SerializedName("result")
    var result: String? = null,
    @SerializedName("confidenceScore")
    var confidenceScore: Double? = null,
    @SerializedName("isAboveThreshold")
    var isAboveThreshold: Boolean? = null,
    @SerializedName("createdAt")
    var createdAt: String? = null
)