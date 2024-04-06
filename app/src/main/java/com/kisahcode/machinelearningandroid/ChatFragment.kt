package com.kisahcode.machinelearningandroid

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kisahcode.machinelearningandroid.databinding.FragmentChatBinding
import java.util.Calendar

/**
 * A fragment representing the chat interface where users can send and receive messages.
 */
class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding

    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable options menu for this fragment
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using FragmentChatBinding
        binding = FragmentChatBinding.inflate(inflater, container, false)

        // Set up the toolbar as the support action bar
        (activity as AppCompatActivity).setSupportActionBar(binding.topAppBar as Toolbar)

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView for chat history
        val historyLayoutManager = LinearLayoutManager(context)
        binding.rvChatHistory.layoutManager = historyLayoutManager

        // Initialize a new instance of ChatHistoryAdapter and assign to the RecyclerView
        val chatAdapter = ChatHistoryAdapter()
        binding.rvChatHistory.adapter = chatAdapter

        // Set up RecyclerView for smart reply options
        val optionsLayoutManager = LinearLayoutManager(context)
        optionsLayoutManager.orientation = RecyclerView.HORIZONTAL
        binding.rvSmartReplyOptions.layoutManager = optionsLayoutManager

        // Initialize a new instance of ReplyOptionsAdapter with an onItemClickCallback implementation
        val replyOptionsAdapter =
            ReplyOptionsAdapter(object : ReplyOptionsAdapter.OnItemClickCallback {
                override fun onOptionClicked(optionText: String) {
                    // Set the clicked option text to the input text field
                    binding.tietInputTextEditText.setText(optionText)
                }
            })
        // Assign the replyOptionsAdapter to the RecyclerView responsible for displaying smart reply options
        binding.rvSmartReplyOptions.adapter = replyOptionsAdapter

        // Obtain an instance of ChatViewModel
        chatViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        // Set initial chat history if not available
        if (chatViewModel.chatHistory.value == null) {
            val chatHistory = ArrayList<Message>()
            chatHistory.add(
                Message(
                    "Hello friend. How are you today?",
                    false,
                    System.currentTimeMillis()
                )
            )
            chatViewModel.setMessages(chatHistory)
        }

        // Observe LiveData objects from ViewModel for updating UI
        chatViewModel.chatHistory.observe(viewLifecycleOwner) { messages ->
            chatAdapter.setChatHistory(messages)
            if (chatAdapter.itemCount > 0) {
                binding.rvChatHistory.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        // Observe smart reply options from ViewModel and update the adapter
        chatViewModel.smartReplyOptions.observe(viewLifecycleOwner) { options ->
            replyOptionsAdapter.setReplyOptions(options)
        }

        // Observe user role changes and update UI accordingly
        chatViewModel.pretendingAsAnotherUser.observe(viewLifecycleOwner) { isPretendingAsAnotherUser ->
            if (isPretendingAsAnotherUser) {
                binding.tvCurrentUser.text = requireContext().getText(R.string.chatting_as_evans)
                binding.tvCurrentUser.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.red
                    )
                )
            } else {
                binding.tvCurrentUser.text = requireContext().getText(R.string.chatting_as_kai)
                binding.tvCurrentUser.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.blue
                    )
                )
            }
        }

        // Observe error messages from ViewModel and display toast messages
        chatViewModel.errorMessage.observe(viewLifecycleOwner) {
            if (it != null)
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        // Hide keyboard when RecyclerView is touched
        binding.rvChatHistory.setOnTouchListener { v, _ ->
            val imm = requireContext().getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            false
        }

        // Hide keyboard when smart reply options RecyclerView is touched
        binding.rvSmartReplyOptions.setOnClickListener { v ->
            val imm = requireContext().getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }

        // Set click listener for switching users
        binding.btnSwitchUser.setOnClickListener {
            chatAdapter.pretendingAsAnotherUser = !chatAdapter.pretendingAsAnotherUser
            chatViewModel.switchUser()
        }

        // Set click listener for sending message
        binding.btnSend.setOnClickListener {
            val input = binding.tietInputTextEditText.text.toString()
            if (input.isNotEmpty()) {
                chatViewModel.addMessage(input)
                binding.tietInputTextEditText.text?.clear()

                val imm = requireContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }

    }

    /**
     * Initialize the options menu for this fragment.
     *
     * @param menu The options menu in which you place your items.
     * @param inflater The MenuInflater object used to inflate the menu layout.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Inflate the options menu for this fragment
        inflater.inflate(R.menu.chat_menu_options, menu)
    }

    /**
     * Handle options menu item selection.
     *
     * @param item The selected menu item.
     * @return Boolean value indicating whether the menu item selection was handled successfully.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.generateBasicChatHistory -> {
                generateBasicChatHistory()
                true
            }

            R.id.generateSensitiveChatHistory -> {
                generateSensitiveChatHistory()
                true
            }

            R.id.clearChatHistory -> {
                chatViewModel.setMessages(ArrayList())
                true
            }

            else -> false
        }
    }

    /**
     * Generate a basic chat history consisting of sample messages.
     */
    private fun generateBasicChatHistory() {
        // Create a list to hold the chat history
        val chatHistory = ArrayList<Message>()
        val calendar = Calendar.getInstance() // Get the current time

        // Add a message "Hello" sent by the local user 10 minutes ago
        calendar.add(Calendar.MINUTE, -10)
        chatHistory.add(Message("Hello", true, calendar.timeInMillis))

        // Add a message "Hey" received from another user 10 minutes after the previous message
        calendar.add(Calendar.MINUTE, 10)
        chatHistory.add(Message("Hey", false, calendar.timeInMillis))

        // Set the generated chat history to the ViewModel
        chatViewModel.setMessages(chatHistory)

    }

    /**
     * Generate a sensitive chat history with emotionally charged messages.
     */
    private fun generateSensitiveChatHistory() {
        // Create a list to hold the chat history
        val chatHistory = ArrayList<Message>()
        val calendar = Calendar.getInstance() // Get the current time

        // Add a message "Hi" received from another user 10 minutes ago
        calendar.add(Calendar.MINUTE, -10)
        chatHistory.add(Message("Hi", false, calendar.timeInMillis))

        // Add a message "How are you?" sent by the local user 1 minute after the previous message
        calendar.add(Calendar.MINUTE, 1)
        chatHistory.add(Message("How are you?", true, calendar.timeInMillis))

        // Add a message "My cat died" received from another user 10 minutes after the previous message
        calendar.add(Calendar.MINUTE, 10)
        chatHistory.add(Message("My cat died", false, calendar.timeInMillis))

        // Set the generated chat history to the ViewModel
        chatViewModel.setMessages(chatHistory)
    }
}