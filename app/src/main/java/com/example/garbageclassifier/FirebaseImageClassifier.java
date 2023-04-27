package com.example.garbageclassifier;

import android.app.Activity;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FirebaseImageClassifier {

    private FirebaseFunctions mFunctions;

    public FirebaseImageClassifier(Activity activity) {

    }

    public List<Recognition> recognizeImage(Bitmap ibitmap) {
        Bitmap bitmap = ibitmap;
        bitmap = scaleBitmapDown(bitmap, 640);
        List<Recognition> recognitions = new ArrayList<>();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        mFunctions = FirebaseFunctions.getInstance();

        // Create json request to cloud vision
        JsonObject request = new JsonObject();
        // Add image to request
        JsonObject image = new JsonObject();
        image.add("content", new JsonPrimitive(base64encoded));
        request.add("image", image);
        //Add features to the request
        JsonObject feature = new JsonObject();
        feature.add("maxResults", new JsonPrimitive(5));
        feature.add("type", new JsonPrimitive("LABEL_DETECTION"));
        JsonArray features = new JsonArray();
        features.add(feature);
        request.add("features", features);

        annotateImage(request.toString())
                .addOnCompleteListener(new OnCompleteListener<JsonElement>() {
                    @Override
                    public void onComplete(@NonNull Task<JsonElement> task) {
                        if (!task.isSuccessful()) {
                            System.out.println("TASK FAILED");
                        } else {
                            for (JsonElement label : task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("labelAnnotations").getAsJsonArray()) {
                                JsonObject labelObj = label.getAsJsonObject();
                                String text = labelObj.get("description").getAsString();
                                String entityID = labelObj.get("mid").getAsString();
                                float score = labelObj.get("score").getAsFloat();
                                recognitions.add(new Recognition(text, score));
                                Log.d("test", entityID + " " + score);
                                System.out.println("Entity" + entityID + "--" + score);
                            }

                        }
                    }
                });
        return recognitions;
    }

    private Task<JsonElement> annotateImage(String requestJson) {
        return mFunctions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith(new Continuation<HttpsCallableResult, JsonElement>() {
                    @Override
                    public JsonElement then(@NonNull Task<HttpsCallableResult> task) {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        return JsonParser.parseString(new Gson().toJson(task.getResult().getData()));
                    }
                });
    }


    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    class Recognition{
        private String name;
        private float confidence;

        public Recognition(String name, float confidence) {
            this.name = name;
            this.confidence = confidence;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return "Recognition {" + "name=" + name + ", confidence=" + confidence + '}';
        }

    }

}



