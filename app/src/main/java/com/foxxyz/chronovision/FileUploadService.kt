package com.foxxyz.chronovision

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver

import java.io.IOException
import java.util.concurrent.TimeUnit

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class FileUploadService : IntentService("FileUploadService") {

    override fun onHandleIntent(intent: Intent?) {
        val url = intent!!.getStringExtra("url")
        val authToken = intent.getStringExtra("authToken")
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        val photo = intent.getParcelableExtra<Uri>("photo")

        // Set receiver
        val receiver = intent.getParcelableExtra<ResultReceiver>("receiver")

        // Inform of progress
        val b = Bundle()
        receiver.send(RUNNING, Bundle.EMPTY)

        // Start the request
        try {
            val mType = "image/jpg".toMediaTypeOrNull()!!
            val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("date", intent.getStringExtra("date"))
                    .addFormDataPart("photo", "photo.jpg", InputStreamRequestBody(mType, contentResolver, photo))
            val request = Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + authToken)
            post(builder, request, body)
            receiver.send(FINISHED, Bundle.EMPTY)
        } catch (e: IOException) {
            e.printStackTrace()
            b.putString(Intent.EXTRA_TEXT, e.toString())
            receiver.send(ERROR, b)
        }

    }

    @Throws(IOException::class)
    fun post(builder: OkHttpClient.Builder, requestBuilder: Request.Builder, bodyBuilder: MultipartBody.Builder): Response {
        val client = builder.build()
        val request = requestBuilder.post(bodyBuilder.build()).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        return response
    }

    companion object {
        val RUNNING = 1
        val ERROR = 2
        val FINISHED = 3
    }
}
