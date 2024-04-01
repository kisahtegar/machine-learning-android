package com.kisahcode.machinelearningandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.kisahcode.machinelearningandroid.databinding.ActivityMainBinding

/**
 * MainActivity class represents the main activity of the application.
 * It extends AppCompatActivity and initializes a canvas to draw objects.
 *
 * @property binding ActivityMainBinding: View binding object for accessing views in the layout.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * onCreate function initializes the activity.
     * It sets the content view to the layout defined in activity_main.xml,
     * creates a bitmap, draws objects on canvas, and adds text.
     *
     * @param savedInstanceState Bundle?: If non-null, this activity is being re-constructed from a previous saved state as given here.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Create a bitmap to draw on
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        binding.myImageView.setImageBitmap(bitmap)

        // Initialize a canvas with the bitmap
        val canvas = Canvas(bitmap)

        // Draw a blue background on the canvas
        canvas.drawColor(ResourcesCompat.getColor(resources, R.color.blue_500, null))

        // Initialize paint for drawing shapes
        val paint = Paint()

        // Save the current canvas state
        canvas.save()

        // Clip the canvas region
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            canvas.clipRect(bitmap.width/2 - 100F, bitmap.height/2 - 100F, bitmap.width/2 + 100F, bitmap.height/2 + 100F, Region.Op.DIFFERENCE)
        } else {
            canvas.clipOutRect(bitmap.width/2 - 100, bitmap.height/2 - 100, bitmap.width/2 + 100, bitmap.height/2 + 100)
        }

        // Draw a pink circle at the center of the canvas
        paint.color = ResourcesCompat.getColor(resources, R.color.pink_200, null)
        canvas.drawCircle((bitmap.width/2).toFloat(), (bitmap.height/2).toFloat(), 200f, paint)

        // Restore the previous canvas state
        canvas.restore()

        // Initialize paint for drawing text
        val paintText =  Paint(Paint.FAKE_BOLD_TEXT_FLAG)
        paintText.textSize = 20F
        paintText.color = ResourcesCompat.getColor(resources, R.color.white, null)

        val text = "Selamat Datang!"
        val bounds = Rect()
        paintText.getTextBounds(text, 0, text.length, bounds)

        // Calculate text position
        val x: Int = bitmap.width/2 - bounds.centerX()
        val y: Int = bitmap.height/2 - bounds.centerY()

        // Draw text on canvas
        canvas.drawText(text, x.toFloat(), y.toFloat(), paintText)
    }
}

/**
 * CustomView class represents a custom view for drawing objects on a canvas.
 * It extends View and provides functionalities to draw shapes and text.
 *
 * @param context Context: The context in which the view is created.
 */
class CustomView(context: Context) : View(context) {

    // Bitmap to hold the drawing content
    private val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)

    // Paint object for drawing shapes
    private val paint = Paint()

    // Paint object for drawing text with bold style
    private val paintText =  Paint(Paint.FAKE_BOLD_TEXT_FLAG)

    // Bounds for measuring text dimensions
    private val bounds = Rect()

    /**
     * onMeasure function determines the size requirements for the view.
     * It sets the measured dimension to the size of the bitmap.
     *
     * @param widthMeasureSpec Int: horizontal space requirements as imposed by the parent.
     * @param heightMeasureSpec Int: vertical space requirements as imposed by the parent.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(bitmap.width, bitmap.height)
    }

    /**
     * onDraw function draws objects on the canvas.
     * It draws a colored circle with a clipped region and adds text.
     *
     * @param canvas Canvas: the canvas on which the objects will be drawn.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Clear canvas with blue color
        canvas.drawColor(ResourcesCompat.getColor(resources, R.color.blue_500, null))

        // Save the current canvas state
        canvas.save()

        // Clip the canvas region
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            canvas.clipRect(bitmap.width/2 - 100F, bitmap.height/2 - 100F, bitmap.width/2 + 100F, bitmap.height/2 + 100F, Region.Op.DIFFERENCE)
        } else {
            canvas.clipOutRect(bitmap.width/2 - 100, bitmap.height/2 - 100, bitmap.width/2 + 100, bitmap.height/2 + 100)
        }

        // Draw a pink circle at the center of the canvas
        paint.color = ResourcesCompat.getColor(resources, R.color.pink_200, null)
        canvas.drawCircle((bitmap.width/2).toFloat(), (bitmap.height/2).toFloat(), 200f, paint)

        // Restore the previous canvas state
        canvas.restore()

        // Set paint settings for drawing text
        paintText.textSize = 20F
        paintText.color = ResourcesCompat.getColor(resources, R.color.white, null)

        // Get text bounds for positioning
        val text = "Selamat Datang!"
        paintText.getTextBounds(text, 0, text.length, bounds)

        // Calculate text position
        val x: Int = bitmap.width/2 - bounds.centerX()
        val y: Int = bitmap.height/2 - bounds.centerY()

        // Draw text on canvas
        canvas.drawText(text, x.toFloat(), y.toFloat(), paintText)
    }
}