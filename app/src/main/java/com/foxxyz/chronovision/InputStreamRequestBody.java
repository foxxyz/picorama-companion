package com.foxxyz.chronovision;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

public class InputStreamRequestBody extends RequestBody {
    private final MediaType contentType;
    private final ContentResolver contentResolver;
    private final Uri uri;

    public InputStreamRequestBody(MediaType contentType, ContentResolver contentResolver, Uri uri) {
        if (uri == null) throw new NullPointerException("Uri is null");
        this.contentType = contentType;
        this.contentResolver = contentResolver;
        this.uri = uri;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() throws IOException {
        return -1;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        sink.writeAll(Okio.source(contentResolver.openInputStream(uri)));
    }
}
