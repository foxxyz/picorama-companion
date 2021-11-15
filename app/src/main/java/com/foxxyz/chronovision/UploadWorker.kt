package com.foxxyz.chronovision

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters

import java.io.IOException
import java.util.concurrent.TimeUnit

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class UploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val authToken = inputData.getString("authToken") ?: return Result.failure()
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        val photo = inputData.getString("photo") ?: return Result.failure()
        val date = inputData.getString("date") ?: return Result.failure()

        // Start the request
        return try {
            val mType = "image/jpg".toMediaTypeOrNull()!!
            val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("date", date)
                    .addFormDataPart("photo", "photo.jpg", InputStreamRequestBody(mType, applicationContext.contentResolver, Uri.parse(photo)))
            val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $authToken")
            post(builder, request, body)
            Result.success()
        } catch (e: IOException) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {

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
