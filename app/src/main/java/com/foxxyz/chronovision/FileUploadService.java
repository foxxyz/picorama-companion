package com.foxxyz.chronovision;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileUploadService extends IntentService {
    public static final int RUNNING = 1;
    public static final int ERROR = 2;
    public static final int FINISHED = 3;

    public FileUploadService() {
        super("FileUploadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String url = intent.getStringExtra("url");
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);
        Uri photo = intent.getParcelableExtra("photo");

        // Set receiver
        final ResultReceiver receiver = intent.getParcelableExtra("receiver");

        // Inform of progress
        Bundle b = new Bundle();
        receiver.send(RUNNING, Bundle.EMPTY);

        // Start the request
        try {
            MultipartBody.Builder body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("date", intent.getStringExtra("date"))
                    .addFormDataPart("photo", "photo.jpg", new InputStreamRequestBody(MediaType.parse("image/jpg"), getContentResolver(), photo));
            Request.Builder request = new Request.Builder().url(url);
            Response response = post(builder, request, body);
            receiver.send(FINISHED, Bundle.EMPTY);
        }
        catch (IOException e) {
            e.printStackTrace();
            b.putString(Intent.EXTRA_TEXT, e.toString());
            receiver.send(ERROR, b);
        }
    }

    public Response post(OkHttpClient.Builder builder, Request.Builder requestBuilder, MultipartBody.Builder bodyBuilder) throws IOException {
        OkHttpClient client = builder.build();
        Request request = requestBuilder.post(bodyBuilder.build()).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        return response;
    }
}
