package com.foxxyz.chronovision;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class UploadActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private static final String UPLOAD_URL = "http://192.168.1.5:8000/add/";
    private final OkHttpClient client = new OkHttpClient();
    private final Calendar calendar = Calendar.getInstance();
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Get incoming intent
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            handleSendImage(intent);
        }

        Button selectPhotoButton = (Button) findViewById(R.id.pick_photo_button);
        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        final DatePickerDialog.OnDateSetListener dateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDate();
            }
        };

        EditText dateField = (EditText) findViewById(R.id.photo_date);
        dateField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(UploadActivity.this, dateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        Button postButton = (Button) findViewById(R.id.post_submit);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            updateImage(uri);
        }
    }

    private void handleSendImage(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        // Put image into the photo picker
        updateImage(uri);
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    private void submit() {
        Intent submit = new Intent(this, FileUploadService.class);
        submit.putExtra("url", UPLOAD_URL);
        EditText dateField = (EditText) findViewById(R.id.photo_date);
        submit.putExtra("date", dateField.getText() + "T12:00");
        submit.putExtra("photo", imageUri);
        startService(submit);
        System.out.println("Submitted");
    }

    private void updateDate() {
        String format = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        EditText dateField = (EditText) findViewById(R.id.photo_date);
        dateField.setText(sdf.format(calendar.getTime()));
    }

    private void updateImage(Uri img) {
        System.out.println("Got " + img.toString());
        imageUri = img;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), img);
            ImageView imageView = (ImageView) findViewById(R.id.photo_preview);
            imageView.setImageBitmap(bitmap);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

}
