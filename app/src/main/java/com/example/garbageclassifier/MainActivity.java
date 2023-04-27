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
import android.widget.TextView;

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
    private TextView predictionMain;

    private Uri imageUri;
    private ImageClassifier imageClassifier;
    private MLKitClassifier mlClassifier;

    //Creates buttons/UI for application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        //listView = findViewById(R.id.listView);
        predictionMain = findViewById(R.id.predictionMain);
        Button captureBt = findViewById(R.id.capture);
        Button galBt = findViewById(R.id.gallery);
        Button mapBt = findViewById(R.id.mapbt);

        try {
            imageClassifier = new ImageClassifier(this);
            mlClassifier = new MLKitClassifier(this);
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

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //IMAGE CAPTURE/CAMERA
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                //convert image to bitmap and call recognizeImage
                Bitmap img = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                ImageView imgv = findViewById(R.id.imageView);
                imgv.setImageBitmap(img);

                List<ImageClassifier.Recognition> predictions = imageClassifier.recognizeImage(img, 0);
                List<String> predictionsList = new ArrayList<>();
                for (ImageClassifier.Recognition recog : predictions) {
                    predictionsList.add("Label: " + recog.getName() + " Confidence: " + recog.getConfidence());
                }
                //ArrayAdapter<String> predictionsAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, predictionsList);
                //listView.setAdapter(predictionsAdapter);
                String prediction = predictions.get(0).getName();
                String tip = "";
                if (prediction.equals("paper"))
                    tip = "All paper is safe to recycle!";
                else if (predictions.equals("plastic"))
                    tip = "Please not that only rigid plastics are accepted for recycling.";
                else if (predictions.equals("metal"))
                    tip = "Please note that electronic devices are not accepeted for recycling.";
                else if (predictions.equals("glass"))
                    tip = "Only glass bottles and jars may be recycled as glass";
                predictionMain.setText(prediction + " --- Confidence: " + predictions.get(0).getConfidence() + "\n" + tip);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        //GALLERY
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageData = data.getData();
            ImageView imgv = findViewById(R.id.imageView);
            imgv.setImageURI(imageData);

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                List<ImageClassifier.Recognition> predictions = imageClassifier.recognizeImage(bitmap, 0);
                List<MLKitClassifier.Recognition> predictions2 = mlClassifier.recognizeImage(bitmap, 0);
                List<String> predictionsList = new ArrayList<>();
                for (ImageClassifier.Recognition recog : predictions) {
                    predictionsList.add("Label: " + recog.getName() + " Confidence: " + recog.getConfidence());
                }
                //ArrayAdapter<String> predictionsAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, predictionsList);
                //listView.setAdapter(predictionsAdapter);
                String prediction = predictions.get(0).getName();
                String tip = "";
                if (prediction.equals("paper")) {
                    tip = "All paper is safe to recycle!";
                }
                else if (predictions.equals("plastic")) {
                    tip = "Please not that only rigid plastics are accepted for recycling.";
                }
                else if (predictions.equals("metal")) {
                    tip = "Please note that electronic devices are not accepeted for recycling.";
                }
                else if (predictions.equals("glass")) {
                    tip = "Only glass bottles and jars may be recycled as glass";
                }
                predictionMain.setText(prediction + " --- Confidence: " + predictions.get(0).getConfidence() + "\n" + tip);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //For when you want to take a picture
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