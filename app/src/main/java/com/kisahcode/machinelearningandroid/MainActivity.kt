package com.kisahcode.machinelearningandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

/**
 * The main activity of the application responsible for initializing the chat fragment.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the fragment manager
        val fragmentManager = supportFragmentManager
        // Create an instance of the ChatFragment
        val chatFragment = ChatFragment()
        // Find the existing fragment by its tag
        val fragment = fragmentManager.findFragmentByTag(ChatFragment::class.java.simpleName)

        // If the fragment is not an instance of ChatFragment, add it to the activity
        if (fragment !is ChatFragment) {
            Log.d("MyChatFragment", "Fragment Name :" + ChatFragment::class.java.simpleName)
            fragmentManager
                .beginTransaction()
                .add(R.id.container, chatFragment, ChatFragment::class.java.simpleName)
                .commit()
        }
    }
}