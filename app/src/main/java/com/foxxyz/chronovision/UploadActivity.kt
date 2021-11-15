package com.foxxyz.chronovision

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.work.*

import java.io.IOException
import java.util.Locale
import java.lang.Integer.parseInt

import org.mindrot.jbcrypt.BCrypt

class UploadActivity : AppCompatActivity()  {
    private val calendar = Calendar.getInstance()
    private var receiver: APIReceiver? = null
    private var imageUri: Uri? = null
    private var preferences: SharedPreferences? = null
    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        uri?.let { it ->
            updateImage(it)
            updateDate()
            toggleInterface(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get incoming intent
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            handleSendImage(intent)
        }

        val selectPhotoButton = findViewById<View>(R.id.photo_preview_container) as RelativeLayout
        selectPhotoButton.setOnClickListener {
            gallery.launch("image/*")
        }

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
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

        // Get preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    // Draw the options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    public override fun onPause() {
        super.onPause()
        if (receiver == null) return
        receiver!!.setReceiver(null)
    }

    private fun handleSendImage(intent: Intent) {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        // Put image into the photo picker
        updateImage(uri)
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
        val url = preferences?.getString("serverURL", "")
        if (url == "") {
            Toast.makeText(this, "Please set site URL!", Toast.LENGTH_LONG).show()
            return
        }
        val authToken = preferences?.getString("authCode", "")
        if (authToken == "") {
            Toast.makeText(this, "Please set server auth code!", Toast.LENGTH_LONG).show()
            return
        }
        val inputs = Data.Builder()
        inputs.putString("url", "$url/add/")
        inputs.putString("authToken", BCrypt.hashpw(authToken, BCrypt.gensalt()))
        val dateField = findViewById<View>(R.id.photo_date) as EditText
        inputs.putString("date", dateField.text.toString() + "T12:00")
        inputs.putString("photo", imageUri.toString())

        // Start and lock interface
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(inputs.build())
                .build()
        WorkManager.getInstance(this).enqueue(request)
        toggleInterface(false)

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
                .observe(this, Observer { info ->
                    if (info == null || !info.state.isFinished) return@Observer
                    if (info.state == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(this, "Photo posted!", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(this, "Unable to contact server! Check your server URL and try again.", Toast.LENGTH_LONG).show()
                        println(info.outputData.getString("error"))
                    }
                    toggleInterface(true)
                })
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
        val `in` = contentResolver.openInputStream((img!!))!!
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            val exifInterface = ExifInterface(`in`)
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            val photoDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)!!
            println(photoDate)
            val parts = photoDate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            // Set calendar to photo date
            calendar.set(Calendar.YEAR, parseInt(parts[0]))
            calendar.set(Calendar.MONTH, parseInt(parts[1]) - 1)
            calendar.set(Calendar.DAY_OF_MONTH, parseInt(parts[2]))
        } catch (e: IOException) {
            println("Could not get date attribute from photo")
        } finally {
            try {
                `in`.close()
            } catch (ignored: IOException) {
            }
        }

        // Display image
        try {
            val source = ImageDecoder.createSource(contentResolver, img)
            val bitmap = ImageDecoder.decodeBitmap(source)
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

    companion object

}
