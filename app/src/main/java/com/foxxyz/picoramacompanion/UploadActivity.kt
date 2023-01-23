package com.foxxyz.picoramacompanion

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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.work.*

import java.io.IOException
import java.util.Locale
import java.lang.Integer.parseInt

import org.mindrot.jbcrypt.BCrypt

class UploadActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener  {
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

        // Update interface based on preferences
        updateInterface()

        if (intent?.type?.startsWith("image/") == true) {
            handleSendImage(intent)
        }
    }

    // Draw the options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val updater = preferences?.edit()
        updater?.putInt("currentTarget", position)
        updater?.commit()
    }

    // Run if app is resumed with new shared content
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.type?.startsWith("image/") == true) {
            handleSendImage(intent)
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

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

    public override fun onResume() {
        super.onResume()
        // Prefs may have been updated
        updateInterface()
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
        val targetSelector = findViewById<Spinner>(R.id.target_selector_spinner)
        val siteNumber = targetSelector.selectedItemPosition + 1

        val url = preferences?.getString("serverURL$siteNumber", "")
        if (url == "") {
            Toast.makeText(this, "Please set site URL!", Toast.LENGTH_LONG).show()
            return
        }
        val authToken = preferences?.getString("authCode$siteNumber", "")
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
            val parts = photoDate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            // Set calendar to photo date
            calendar.set(Calendar.YEAR, parseInt(parts[0]))
            calendar.set(Calendar.MONTH, parseInt(parts[1]) - 1)
            calendar.set(Calendar.DAY_OF_MONTH, parseInt(parts[2]))
            updateDate()
        } catch (e: IOException) {
            Toast.makeText(this, "Unable to get date from photo! Using current date.", Toast.LENGTH_LONG).show()
        } catch (e: NullPointerException) {
            Toast.makeText(this, "Unable to get date from photo! Using current date.", Toast.LENGTH_LONG).show()
        } finally {
            try {
                `in`.close()
            } catch (ignored: IOException) {
                Toast.makeText(this, "IO exception accessing photo", Toast.LENGTH_LONG).show()
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

    private fun updateInterface() {
        // Get preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val prefs = preferences!!

        // Show available targets if multiple specified
        val targetOptions = mutableListOf(prefs.getString("serverName1", "Untitled"))
        for(i in 2..3) {
            val target = prefs.getString("serverName$i", "")
            if (target == "") continue
            targetOptions.add(target)
        }
        val targetAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, targetOptions)
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val targetSelector = findViewById<Spinner>(R.id.target_selector_spinner)
        // Make sure we don't select more than we have
        var selectedTargetPosition = prefs.getInt("currentTarget", 0)
        selectedTargetPosition = Integer.min(selectedTargetPosition, targetOptions.size - 1)
        with(targetSelector) {
            adapter = targetAdapter
            setSelection(selectedTargetPosition, false)
            onItemSelectedListener = this@UploadActivity
            prompt = "Select Site Target"
        }

        // Hide if only one site available
        val targetSelectorWrapper = findViewById<LinearLayout>(R.id.target_selector)
        targetSelectorWrapper.visibility = if (targetOptions.size > 1) android.view.View.VISIBLE else android.view.View.GONE
    }

    companion object

}
