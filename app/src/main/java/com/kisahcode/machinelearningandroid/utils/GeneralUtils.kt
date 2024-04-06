package com.kisahcode.machinelearningandroid.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.kisahcode.machinelearningandroid.R

/**
 * Retrieves the user profile icon drawable with tint color based on the user type.
 *
 * @param context The context used to access resources.
 * @param isLocalUser Boolean flag indicating whether the user is local or remote.
 * @return The profile icon drawable with tint color applied.
 * @throws IllegalStateException if the user profile image drawable cannot be retrieved.
 */
fun getProfileIcon(context: Context, isLocalUser: Boolean): Drawable {
    // Retrieve the default profile icon drawable
    val drawable =
        ContextCompat.getDrawable(context, R.drawable.ic_tag_faces_black_24dp)
            ?: throw IllegalStateException("Could not get user profile image")

    // Apply tint color based on the user type
    if (isLocalUser) {
        // Tint the drawable with blue color for local user
        DrawableCompat.setTint(drawable.mutate(), Color.BLUE)
    } else {
        // Tint the drawable with red color for remote user
        DrawableCompat.setTint(drawable.mutate(), Color.RED)
    }

    return drawable
}