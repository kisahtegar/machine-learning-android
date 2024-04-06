package com.kisahcode.machinelearningandroid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kisahcode.machinelearningandroid.databinding.ItemOptionsSmartreplyBinding
import com.google.mlkit.nl.smartreply.SmartReplySuggestion

/**
 * Adapter for displaying Smart Reply options in a RecyclerView.
 * It binds the SmartReplySuggestion data to the ViewHolder and handles item click events.
 *
 * @param onItemClickCallback The callback interface for handling item click events.
 */
class ReplyOptionsAdapter(
    private val onItemClickCallback: OnItemClickCallback
) : RecyclerView.Adapter<ReplyOptionsAdapter.ViewHolder>() {

    private val smartReplyOptions = ArrayList<SmartReplySuggestion>()

    /**
     * Interface for handling item click events in the RecyclerView.
     */
    interface OnItemClickCallback {
        /**
         * Called when an option in the RecyclerView is clicked.
         * @param optionText The text of the clicked option.
         */
        fun onOptionClicked(optionText: String)
    }

    /**
     * ViewHolder for displaying each Smart Reply option.
     *
     * @property binding The view binding object for the item layout.
     */
    inner class ViewHolder(val binding: ItemOptionsSmartreplyBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Creates a new ViewHolder instance by inflating the item layout.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        // Inflate the item layout for each Smart Reply option
        val binding = ItemOptionsSmartreplyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    /**
     * Binds data to the ViewHolder by setting the text of the Smart Reply option and adding a click listener.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ReplyOptionsAdapter.ViewHolder, position: Int) {
        // Get the text of the Smart Reply option at the specified position
        val optionText = smartReplyOptions[position].text
        // Set the text of the Smart Reply option to the TextView in the ViewHolder's binding
        holder.binding.tvSmartReplyOption.text = optionText
        // Add a click listener to the item view to handle option selection
        holder.itemView.setOnClickListener {
            // Pass the selected option text to the onItemClickCallback
            onItemClickCallback.onOptionClicked(optionText)
        }
    }

    /**
     * Returns the total number of Smart Reply options.
     *
     * @return The total number of Smart Reply options in the adapter's data set.
     */
    override fun getItemCount(): Int {
        return smartReplyOptions.size
    }

    /**
     * Sets the Smart Reply options to be displayed in the adapter and notifies any registered
     * observers that the data set has changed.
     *
     * @param smartReplyOptions The list of Smart Reply options to be set.
     */
    fun setReplyOptions(smartReplyOptions: List<SmartReplySuggestion>) {
        this.smartReplyOptions.clear()
        this.smartReplyOptions.addAll(smartReplyOptions)
        notifyDataSetChanged()
    }
}