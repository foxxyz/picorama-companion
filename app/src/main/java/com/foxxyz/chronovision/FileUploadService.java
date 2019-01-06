package com.foxxyz.chronovision;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploadService extends IntentService {
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
        try {
            MultipartBody.Builder body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("date", intent.getStringExtra("date"))
                    .addFormDataPart("photo", "photo.jpg", new InputStreamRequestBody(MediaType.parse("image/jpg"), getContentResolver(), photo));
            Request.Builder request = new Request.Builder().url(url);
            post(builder, request, body);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Response post(OkHttpClient.Builder builder, Request.Builder requestBuilder, MultipartBody.Builder bodyBuilder) throws IOException {
        OkHttpClient client = builder.build();
        Request request = requestBuilder.post(bodyBuilder.build()).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        System.out.println("Here we go");
        System.out.println(response.toString());
        return response;
    }
}
