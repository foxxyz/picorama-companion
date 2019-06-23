package com.foxxyz.chronovision

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import java.io.IOException
import java.io.InputStream
import java.util.Locale

import java.lang.Integer.parseInt


class UploadActivity : AppCompatActivity(), APIReceiver.Receiver {
    private val calendar = Calendar.getInstance()
    var receiver: APIReceiver? = null
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        // Get incoming intent
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            handleSendImage(intent)
        }

        val selectPhotoButton = findViewById<View>(R.id.photo_preview_container) as RelativeLayout
        selectPhotoButton.setOnClickListener { openGallery() }

        val dateListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDate()
        }

        val dateField = findViewById<View>(R.id.photo_date) as EditText
        dateField.setOnClickListener { DatePickerDialog(this@UploadActivity, dateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() }

        val postButton = findViewById<View>(R.id.post_submit) as Button
        postButton.setOnClickListener { submit() }

        // Set current date
        updateDate()
    }

    public override fun onPause() {
        super.onPause()
        if (receiver == null) return
        receiver!!.setReceiver(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println(requestCode)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val uri = data.data
            updateImage(uri)
            updateDate()
            toggleInterface(true)
        }
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
        when (resultCode) {
            FileUploadService.RUNNING -> {
            }
            FileUploadService.FINISHED -> {
                Toast.makeText(this, "Photo posted!", Toast.LENGTH_LONG).show()
                toggleInterface(true)
            }
            FileUploadService.ERROR -> {
                println("error")
                toggleInterface(true)
            }
        }
    }

    private fun handleSendImage(intent: Intent) {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        // Put image into the photo picker
        updateImage(uri)
    }

    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    // Rotate bitmap to correct orientation
    private fun rotate(source: Bitmap, orientation: Int): Bitmap {
        val transform = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> transform.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> transform.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> transform.postRotate(270f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, transform, true)
    }

    private fun submit() {
        val submit = Intent(this, FileUploadService::class.java)
        submit.putExtra("url", UPLOAD_URL)
        val dateField = findViewById<View>(R.id.photo_date) as EditText
        submit.putExtra("date", dateField.text.toString() + "T12:00")
        submit.putExtra("photo", imageUri)

        // Set up receiver
        receiver = APIReceiver(Handler())
        receiver!!.setReceiver(this)
        submit.putExtra("receiver", receiver)

        // Start and lock interface
        startService(submit)
        toggleInterface(false)
    }

    private fun toggleInterface(enabled: Boolean) {
        val postButton = findViewById<View>(R.id.post_submit) as Button
        val animate = AlphaAnimation(if (enabled) 0.2f else 1.0f, if (enabled) 1.0f else 0.2f)
        animate.duration = 500
        animate.fillAfter = true
        postButton.startAnimation(animate)
        postButton.isEnabled = enabled
    }

    private fun updateDate() {
        val format = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(format, Locale.US)
        val dateField = findViewById<View>(R.id.photo_date) as EditText
        dateField.setText(sdf.format(calendar.time))
    }

    private fun updateImage(img: Uri?) {
        imageUri = img

        // Get EXIF date and orientation
        var `in`: InputStream? = null
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            `in` = contentResolver.openInputStream(img!!)
            val exifInterface = ExifInterface(`in`)
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            val photoDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            println(photoDate)
            val parts = photoDate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            // Set calendar to photo date
            calendar.set(Calendar.YEAR, parseInt(parts[0]))
            calendar.set(Calendar.MONTH, parseInt(parts[1]) - 1)
            calendar.set(Calendar.DAY_OF_MONTH, parseInt(parts[2]))
        } catch (e: IOException) {
            println("Could not get date attribute from photo")
        } finally {
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (ignored: IOException) {
                }

            }
        }

        // Display image
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, img)
            val imageView = findViewById<View>(R.id.photo_preview) as ImageView
            imageView.setImageBitmap(rotate(bitmap, orientation))
            imageView.setBackgroundColor(Color.TRANSPARENT)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Hide hint text and background
        val hintText = findViewById<View>(R.id.photo_preview_hint) as TextView
        hintText.visibility = View.INVISIBLE
    }

    companion object {
        private val PICK_IMAGE = 100
        private val UPLOAD_URL = "http://192.168.1.5:8000/add/"
    }

}
