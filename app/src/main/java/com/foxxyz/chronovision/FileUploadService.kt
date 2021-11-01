package com.foxxyz.chronovision

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import androidx.work.Worker

import java.io.IOException
import java.util.concurrent.TimeUnit

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class FileUploadService : Worker {

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
        receiver?.send(RUNNING, Bundle.EMPTY)

        // Start the request
        try {
            val mType = "image/jpg".toMediaTypeOrNull()!!
            val body = intent.getStringExtra("date")?.let {
                MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("date", it)
                        .addFormDataPart("photo", "photo.jpg", InputStreamRequestBody(mType, contentResolver, photo))
            }
            val request = url?.let {
                Request.Builder().url(it)
                        .addHeader("Authorization", "Bearer $authToken")
            }
            if (request != null && body != null) {
                Companion.post(builder, request, body)
            }
            receiver?.send(FINISHED, Bundle.EMPTY)
        } catch (e: IOException) {
            e.printStackTrace()
            b.putString(Intent.EXTRA_TEXT, e.toString())
            receiver?.send(ERROR, b)
        }

    }

    companion object {
        const val RUNNING = 1
        const val ERROR = 2
        const val FINISHED = 3

        @Throws(IOException::class)
        fun post(builder: OkHttpClient.Builder, requestBuilder: Request.Builder, bodyBuilder: MultipartBody.Builder): Response {
            val client = builder.build()
            val request = requestBuilder.post(bodyBuilder.build()).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response
        }
    }
}
