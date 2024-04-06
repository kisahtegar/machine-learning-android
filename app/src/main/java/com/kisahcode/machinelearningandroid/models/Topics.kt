package com.kisahcode.machinelearningandroid.models

import com.google.gson.annotations.SerializedName

/**
 * Represents topics containing titles, contents, and questions.
 *
 * @property titles A list of titles for the topics.
 * @property contents A list of contents for the topics.
 * @property questions A list of questions related to the topics.
 */
data class Topics(
    @SerializedName("titles")
    private val titles: List<List<String>>,
    @SerializedName("contents")
    private val contents: List<List<String>>,
    @SerializedName("questions")
    val questions: List<List<String>>
) {

    /**
     * Retrieves the list of titles for the topics.
     *
     * @return A list of titles.
     */
    fun getTitles(): List<String> {
        return titles.map { it[0] }
    }

    /**
     * Retrieves the list of contents for the topics.
     *
     * @return A list of contents.
     */
    fun getContents(): List<String> {
        return contents.map { it[0] }
    }

}