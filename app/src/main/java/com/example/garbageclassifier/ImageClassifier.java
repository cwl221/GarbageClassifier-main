package com.example.garbageclassifier;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ImageClassifier {

    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final int MAX_SIZE = 5;

    private final List<String> labels;
    private final Interpreter tensorClassifier;
    private final int imageResizeX, imageResizeY;
    private final TensorBuffer probabilityImageBuffer;
    private final TensorProcessor probabilityProcessor;
    private TensorImage imageInputBuffer;
    private MappedByteBuffer classifierModel;
    private List<String> plasticsList, papersList, metalsList, glassList;
    private BufferedReader reader;

    public ImageClassifier(Activity activity) throws IOException {
        classifierModel = FileUtil.loadMappedFile(activity, "mobilenet_v1_1.0_224_quant.tflite");
        labels = FileUtil.loadLabels(activity, "labels_mobilenet_quant_v1_224.txt");

        int imageTensorIndex = 0;
        int probabilityTensorIndex = 0;
        tensorClassifier = new Interpreter(classifierModel, null);
        int[] inputImageShape = tensorClassifier.getInputTensor(imageTensorIndex).shape();
        DataType inputDataType = tensorClassifier.getInputTensor(imageTensorIndex).dataType();
        int[] outputImageShape = tensorClassifier.getOutputTensor(probabilityTensorIndex).shape();
        DataType outputDataType = tensorClassifier.getOutputTensor(probabilityTensorIndex).dataType();

        //assign variables
        imageResizeX = inputImageShape[1];
        imageResizeY = inputImageShape[2];
        imageInputBuffer = new TensorImage(inputDataType);
        probabilityImageBuffer = TensorBuffer.createFixedSize(outputImageShape, outputDataType);
        probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)).build();

        //create lists
        plasticsList = new ArrayList<>();
        papersList = new ArrayList<>();
        metalsList = new ArrayList<>();
        glassList = new ArrayList<>();

        try {
            final InputStream file = activity.getAssets().open("plastics.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                plasticsList.add(line);
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            final InputStream file = activity.getAssets().open("papers.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                papersList.add(line);
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            final InputStream file = activity.getAssets().open("metals.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                metalsList.add(line);
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            final InputStream file = activity.getAssets().open("glass.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                glassList.add(line);
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(plasticsList.toString());
    }

    //output list is list of objects and the confidence
    public List<Recognition> recognizeImage(final Bitmap bitmap, final int sensorOrientation) {
        List<Recognition> recognitions = new ArrayList<>();
        imageInputBuffer = loadImage(bitmap, sensorOrientation);
        tensorClassifier.run(imageInputBuffer.getBuffer(), probabilityImageBuffer.getBuffer().rewind());
        Map<String, Float> labelledProbability = new TensorLabel(labels, probabilityProcessor.process(probabilityImageBuffer)).getMapWithFloatValue();
        for (Map.Entry<String, Float> entry : labelledProbability.entrySet()) {
            String key = entry.getKey();
            Float confidence = entry.getValue();
            if (plasticsList.contains(key))
                key = "plastic";
            else if (papersList.contains(key))
                key = "paper";
            else if (metalsList.contains(key))
                key = "metal";
            else if (glassList.contains(key))
                key = "glass";
            recognitions.add(new Recognition(key, confidence));
        }
        Collections.sort(recognitions); //sort based on confidence
        return recognitions;
    }

    private TensorImage loadImage(Bitmap bitmap, int sensorOrientation) {
        imageInputBuffer.load(bitmap);
        //int rotations = sensorOrientation / 90;
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(imageResizeX, imageResizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new Rot90Op(sensorOrientation))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();
        return imageProcessor.process(imageInputBuffer);
    }

    class Recognition implements Comparable{
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

        @Override
        public int compareTo(Object o) {
            return Float.compare(((Recognition) o).confidence, this.confidence);
        }
    }
}
