package com.kisahcode.machinelearningandroid

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplyGenerator
import com.google.mlkit.nl.smartreply.SmartReplySuggestion
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage

/**
 * ViewModel responsible for managing chat-related data and providing smart reply options.
 *
 * @property anotherUserID The ID of the user in the conversation other than the local user.
 * @property chatHistory LiveData holding the chat history.
 * @property pretendingAsAnotherUser LiveData indicating whether the user is pretending as another user.
 * @property smartReplyOptions LiveData holding the smart reply options.
 * @property errorMessage LiveData holding any error message occurred during smart reply generation.
 * @property smartReply Instance of SmartReplyGenerator for generating smart reply suggestions.
 */
class ChatViewModel : ViewModel() {

    private val anotherUserID = "101"

    private val _chatHistory = MutableLiveData<ArrayList<Message>>()
    val chatHistory: LiveData<ArrayList<Message>> = _chatHistory

    private val _pretendingAsAnotherUser = MutableLiveData<Boolean>()
    val pretendingAsAnotherUser: LiveData<Boolean> = _pretendingAsAnotherUser

    private val smartReply: SmartReplyGenerator = SmartReply.getClient()

    private val _smartReplyOptions = MediatorLiveData<List<SmartReplySuggestion>>()
    val smartReplyOptions: LiveData<List<SmartReplySuggestion>> = _smartReplyOptions

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Initializes the ViewModel by setting up the Smart Reply options generator and default values.
     */
    init {
        initSmartReplyOptionsGenerator()
        _pretendingAsAnotherUser.value = false
    }

    /**
     * Switches the user between local and another user, and clears the smart reply options.
     * If the user is currently pretending as another user, it switches back to the local user,
     * and vice versa.
     */
    fun switchUser() {
        clearSmartReplyOptions()
        val value = _pretendingAsAnotherUser.value!!
        _pretendingAsAnotherUser.value = !value
    }

    /**
     * Sets the messages in the chat history and clears the smart reply options.
     *
     * @param messages The list of messages to set as the chat history.
     */
    fun setMessages(messages: ArrayList<Message>) {
        clearSmartReplyOptions()
        _chatHistory.value = messages
    }

    /**
     * Adds a new message to the chat history and clears the smart reply options.
     * If the user is currently pretending as another user, the message will be added as if it's
     * from the other user, otherwise it will be added as a local user message.
     *
     * @param message The text of the message to be added.
     */
    fun addMessage(message: String) {
        // Determine whether the message should be added as a local user or another user
        val user = _pretendingAsAnotherUser.value!!

        // Get the current chat history
        val list: ArrayList<Message> = chatHistory.value ?: ArrayList()
        // Add the new message to the chat history
        list.add(Message(message, !user, System.currentTimeMillis()))

        // Clear smart reply options
        clearSmartReplyOptions()

        // Update the chat history LiveData with the new list of messages
        _chatHistory.value = list

    }

    /**
     * Clears the smart reply options.
     */
    private fun clearSmartReplyOptions() {
        _smartReplyOptions.value = ArrayList()
    }

    /**
     * Initializes the Smart Reply options generator by observing changes in the chat history
     * and the user's pretending status. This method generates Smart Reply options based on the
     * latest chat history and user status.
     */
    private fun initSmartReplyOptionsGenerator() {
        _smartReplyOptions.addSource(pretendingAsAnotherUser) { isPretendingAsAnotherUser ->
            val list = chatHistory.value

            // Check if the chat history is empty
            if (list.isNullOrEmpty()) {
                return@addSource
            } else {
                // Generate Smart Reply options based on the chat history and user status
                generateSmartReplyOptions(list, isPretendingAsAnotherUser)
                    .addOnSuccessListener { result ->
                        _smartReplyOptions.value = result
                    }
            }

        }

        _smartReplyOptions.addSource(chatHistory) { conversations ->
            val isPretendingAsAnotherUser = pretendingAsAnotherUser.value

            // Check if the chat history is empty
            if (isPretendingAsAnotherUser != null && conversations.isNullOrEmpty()) {
                return@addSource
            } else {
                // Generate Smart Reply options based on the chat history and user status
                generateSmartReplyOptions(conversations, isPretendingAsAnotherUser!!)
                    .addOnSuccessListener { result ->
                        _smartReplyOptions.value = result
                    }
            }

        }
    }

    /**
     * Generates Smart Reply options based on the provided chat messages and user status.
     *
     * @param messages The list of chat messages.
     * @param isPretendingAsAnotherUser Indicates whether the user is pretending as another user.
     * @return A task that will resolve to a list of Smart Reply suggestions.
     */
    private fun generateSmartReplyOptions(
        messages: List<Message>,
        isPretendingAsAnotherUser: Boolean
    ): Task<List<SmartReplySuggestion>> {
        val lastMessage = messages.last()

        // Check if the last message sender matches the current user status
        if (lastMessage.isLocalUser != isPretendingAsAnotherUser) {
            // Return a failed task if the last message sender does not match the user status
            return Tasks.forException(Exception("Tidak menjalankan smart reply!"))
        }

        // Prepare the list of chat conversations for Smart Reply
        val chatConversations = ArrayList<TextMessage>()
        for (message in messages) {
            if (message.isLocalUser != isPretendingAsAnotherUser) {
                // Add local user's messages to chat conversations
                chatConversations.add(
                    TextMessage.createForLocalUser(
                        message.text,
                        message.timestamp
                    )
                )
            } else {
                // Add remote user's messages to chat conversations
                chatConversations.add(
                    TextMessage.createForRemoteUser(message.text, message.timestamp, anotherUserID)
                )
            }
        }

        // Generate Smart Reply suggestions for the chat conversations
        return smartReply
            .suggestReplies(chatConversations)
            .continueWith { task ->
                val result = task.result
                // Handle different Smart Reply result statuses
                when (result.status) {
                    SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE ->
                        _errorMessage.value =
                            "Unable to generate options due to a non-English language was used"

                    SmartReplySuggestionResult.STATUS_NO_REPLY ->
                        _errorMessage.value =
                            "Unable to generate options due to no appropriate response found"
                }
                // Return the Smart Reply suggestions
                result.suggestions
            }
            .addOnFailureListener { e ->
                // Handle failure to generate Smart Reply options
                _errorMessage.value = "An error has occured on Smart Reply Instance"
            }

    }

    /**
     * Called when the ViewModel is being cleared and will no longer be used.
     * It closes the Smart Reply instance to release associated resources.
     */
    override fun onCleared() {
        super.onCleared()
        smartReply.close()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}