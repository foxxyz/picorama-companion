package com.foxxyz.chronovision;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static java.lang.Integer.parseInt;


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
            updateDate();
            toggleInterface(true);
        }
    }

    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch(resultCode) {
            case FileUploadService.RUNNING:
                break;
            case FileUploadService.FINISHED:
                Toast.makeText(this, "Photo posted!", Toast.LENGTH_LONG).show();
                toggleInterface(true);
                break;
            case FileUploadService.ERROR:
                System.out.println("error");
                toggleInterface(true);
                break;
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

    // Rotate bitmap to correct orientation
    private Bitmap rotate(Bitmap source, int orientation) {
        Matrix transform = new Matrix();
        switch(orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                transform.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                transform.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                transform.postRotate(270);
                break;
        }
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), transform, true);
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

        // Start and lock interface
        startService(submit);
        toggleInterface(false);
    }

    private void toggleInterface(boolean enabled) {
        Button postButton = (Button) findViewById(R.id.post_submit);
        AlphaAnimation animate = new AlphaAnimation(enabled ? 0.2f : 1.0f, enabled ? 1.0f : 0.2f);
        animate.setDuration(500);
        animate.setFillAfter(true);
        postButton.startAnimation(animate);
        postButton.setEnabled(enabled);
    }

    private void updateDate() {
        String format = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        EditText dateField = (EditText) findViewById(R.id.photo_date);
        dateField.setText(sdf.format(calendar.getTime()));
    }

    private void updateImage(Uri img) {
        imageUri = img;

        // Get EXIF date and orientation
        InputStream in = null;
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            in = getContentResolver().openInputStream(img);
            ExifInterface exifInterface = new ExifInterface(in);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            String photoDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            System.out.println(photoDate);
            String[] parts = photoDate.split(" ")[0].split(":");
            // Set calendar to photo date
            calendar.set(Calendar.YEAR, parseInt(parts[0]));
            calendar.set(Calendar.MONTH, parseInt(parts[1]) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, parseInt(parts[2]));
        }
        catch(IOException e) {
            System.out.println("Could not get date attribute from photo");
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch(IOException ignored) {}
            }
        }

        // Display image
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), img);
            ImageView imageView = (ImageView) findViewById(R.id.photo_preview);
            imageView.setImageBitmap(rotate(bitmap, orientation));
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
