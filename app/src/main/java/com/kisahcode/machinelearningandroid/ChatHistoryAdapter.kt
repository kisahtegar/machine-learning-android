package com.kisahcode.machinelearningandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kisahcode.machinelearningandroid.utils.getProfileIcon

/**
 * Adapter for displaying chat history messages in a RecyclerView.
 */
class ChatHistoryAdapter : RecyclerView.Adapter<ChatHistoryAdapter.MessageViewHolder>() {

    // List to hold chat history messages.
    private val chatHistory = ArrayList<Message>()

    /**
     * Flag indicating whether the adapter should display messages as if they are sent by another user.
     * When set to true, messages will be displayed as if they are sent by another user.
     */
    var pretendingAsAnotherUser = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**
     * ViewHolder class for individual message items.
     */
    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    /**
     * Creates a new MessageViewHolder instance.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new MessageViewHolder instance.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatHistoryAdapter.MessageViewHolder {
        // Inflate the layout for the message item view
        val messageItemView =
            LayoutInflater.from(parent.context).inflate(viewType, parent, false) as ViewGroup
        return MessageViewHolder(messageItemView)
    }

    /**
     * Binds the data to the views within the MessageViewHolder.
     *
     * @param holder The MessageViewHolder to bind data to.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ChatHistoryAdapter.MessageViewHolder, position: Int) {
        // Retrieve views from the MessageViewHolder
        val userProfile = holder.itemView.findViewById<ImageView>(R.id.iv_userProfile)
        val userMessageText = holder.itemView.findViewById<TextView>(R.id.tv_userMessageText)

        // Get the message at the specified position
        val message = chatHistory[position]

        // Set the user profile icon based on whether the message is from the local user or another user
        userProfile.setImageDrawable(getProfileIcon(userProfile.context, message.isLocalUser))
        // Set the message text
        userMessageText.text = message.text

    }

    /**
     * Determines the view type for the item at the specified position.
     *
     * @param position The position of the item within the adapter's data set.
     * @return The view type of the item at the specified position.
     */
    override fun getItemViewType(position: Int): Int {
        // Check if the message is from the local user and pretending as another user,
        // or if the message is from another user and pretending as the local user
        return if (
            chatHistory[position].isLocalUser && !pretendingAsAnotherUser ||
            !chatHistory[position].isLocalUser && pretendingAsAnotherUser
        ) {
            R.layout.item_message_local
        } else {
            R.layout.item_message_another_user
        }
    }

    /**
     * Returns the total number of items in the chat history.
     *
     * @return The total number of items in the chat history.
     */
    override fun getItemCount(): Int {
        return chatHistory.size
    }

    /**
     * Sets the chat history with the provided list of messages and notifies the adapter of the data set change.
     *
     * @param messages The list of messages to set as the chat history.
     */
    fun setChatHistory(messages: List<Message>) {
        // Clear the existing chat history and add all messages from the provided list
        chatHistory.clear()
        chatHistory.addAll(messages)
        // Notify the adapter that the data set has changed
        notifyDataSetChanged()
    }
}