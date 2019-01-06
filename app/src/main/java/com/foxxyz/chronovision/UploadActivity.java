package com.foxxyz.chronovision;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;


public class UploadActivity extends AppCompatActivity implements APIReceiver.Receiver {
    private static final int PICK_IMAGE = 100;
    private static final String UPLOAD_URL = "http://192.168.1.5:8000/add/";
    private final Calendar calendar = Calendar.getInstance();
    public APIReceiver receiver;
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

        RelativeLayout selectPhotoButton = (RelativeLayout) findViewById(R.id.photo_preview_container);
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

        // Set current date
        updateDate();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (receiver == null) return;
        receiver.setReceiver(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println(requestCode);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            updateImage(uri);
        }
    }

    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch(resultCode) {
            case FileUploadService.RUNNING:
                break;
            case FileUploadService.FINISHED:
                Toast.makeText(this, "Photo posted!", Toast.LENGTH_LONG).show();
                break;
            case FileUploadService.ERROR:
                System.out.println("error");
                break;
        }
    }

    private void handleSendImage(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        // Put image into the photo picker
        System.out.println("Got " + uri);
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

        // Set up receiver
        receiver = new APIReceiver(new Handler());
        receiver.setReceiver(this);
        submit.putExtra("receiver", receiver);

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
        imageUri = img;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), img);
            ImageView imageView = (ImageView) findViewById(R.id.photo_preview);
            imageView.setImageBitmap(bitmap);
            imageView.setBackgroundColor(Color.TRANSPARENT);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        // Hide hint text and background
        TextView hintText = (TextView) findViewById(R.id.photo_preview_hint);
        hintText.setVisibility(View.INVISIBLE);
    }

}
