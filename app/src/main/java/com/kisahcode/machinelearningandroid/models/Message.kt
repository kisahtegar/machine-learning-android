package com.kisahcode.machinelearningandroid.models

/**
 * Represents a message in the chat application.
 *
 * @property text The content of the message.
 * @property isFromUser Indicates whether the message is sent by the user.
 */

data class Message(
    val text: String,
    val isFromUser: Boolean
)