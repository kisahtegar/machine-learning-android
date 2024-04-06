package com.kisahcode.machinelearningandroid

/**
 * Represents a message object in the chat application.
 *
 * @property text The content of the message.
 * @property isLocalUser Boolean flag indicating whether the message is sent by the local user.
 * @property timestamp The timestamp of the message.
 */
data class Message(
    val text: String,
    val isLocalUser: Boolean,
    val timestamp: Long
)