package com.foxxyz.chronovision

import android.content.ContentResolver
import android.net.Uri

import java.io.IOException

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

class InputStreamRequestBody(private val contentType: MediaType, private val contentResolver: ContentResolver, private val uri: Uri?) : RequestBody() {

    init {
        if (uri == null) throw NullPointerException("Uri is null")
    }

    override fun contentType(): MediaType? {
        return contentType
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        sink.writeAll(contentResolver.openInputStream(uri).source())
    }
}
