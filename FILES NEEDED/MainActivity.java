package com.example.garbageclassifier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int GALLERY_REQUEST_CODE = 123;

    private ImageView imageView;
    private ListView listView;

    private Uri imageUri;
    private ImageClassifier imageClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        listView = findViewById(R.id.listView);
        Button captureBt = findViewById(R.id.capture);
        Button galBt = findViewById(R.id.gallery);
        Button mapBt = findViewById(R.id.mapbt);

        try {
            imageClassifier = new ImageClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        captureBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        galBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select an Image"), GALLERY_REQUEST_CODE);
            }
        });

        mapBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(MainActivity.this, PermissionActivity.class);
                startActivity(mapIntent);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                Bitmap img = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                ImageView imgv = findViewById(R.id.imageView);
                imgv.setImageBitmap(img);

                List<ImageClassifier.Recognition> predictions = imageClassifier.recognizeImage(img, 0);
                List<String> predictionsList = new ArrayList<>();
                for (ImageClassifier.Recognition recog : predictions) {
                    predictionsList.add("Label: " + recog.getName() + " Confidence: " + recog.getConfidence());
                }
                ArrayAdapter<String> predictionsAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, predictionsList);
                listView.setAdapter(predictionsAdapter);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageData = data.getData();
            ImageView imgv = findViewById(R.id.imageView);
            imgv.setImageURI(imageData);

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                List<ImageClassifier.Recognition> predictions = imageClassifier.recognizeImage(bitmap, 0);
                List<String> predictionsList = new ArrayList<>();
                for (ImageClassifier.Recognition recog : predictions) {
                    predictionsList.add("Label: " + recog.getName() + " Confidence: " + recog.getConfidence());
                }
                ArrayAdapter<String> predictionsAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, predictionsList);
                listView.setAdapter(predictionsAdapter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        try {
            File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "out_image.jpg");
            try {
                if (photoFile.exists()) {
                    photoFile.delete();
                }
                photoFile.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= 24) {
                imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.cameraapp.fileprovider", photoFile);
            }
            else {
                imageUri = Uri.fromFile(photoFile);
            }

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
        catch (ActivityNotFoundException e) {

        }
    }
}