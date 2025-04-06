package com.example.csci3310project;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.widget.TextView;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.calib3d.Calib3d;

public class Page3AFragment extends Fragment {
    // UI Elements
    private PreviewView previewView;
    private TextView faceTiltView, finalTiltView, deviceTiltView;

    // Camera/Sensor
    private ProcessCameraProvider cameraProvider;
    private SensorManager sensorManager;
    private float devicePitchDegrees = 0;

    // MediaPipe
    private FaceLandmarker faceLandmarker;
    private final MatOfPoint3f objectPoints = new MatOfPoint3f(
            new Point3(0.0f, 0.0f, 0.0f),           // Nose tip
            new Point3(0.0f, -330.0f, -65.0f),      // Chin
            new Point3(-225.0f, 170.0f, -135.0f),   // Left eye left corner
            new Point3(225.0f, 170.0f, -135.0f),    // Right eye right corner
            new Point3(-150.0f, -150.0f, -125.0f),  // Left mouth corner
            new Point3(150.0f, -150.0f, -125.0f)    // Right mouth corner
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.page3a, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewView);
        faceTiltView = view.findViewById(R.id.faceAngle);
        finalTiltView = view.findViewById(R.id.finalAngle);
        deviceTiltView = view.findViewById(R.id.deviceAngle);

        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (OpenCVLoader.initLocal()) {
            Log.i("OpenCV", "OpenCV loaded successfully");
            if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
                // Device doesn't support rotation vector sensor
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Feature Not Available")
                        .setMessage("Your device doesn't support the required sensors for this feature.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            } else {
                // Check camera permissions before starting if not granted request them
                if (cameraPermission()) {
                    startCamera();
                } else {
                    requestPermissions(
                            new String[]{Manifest.permission.CAMERA},
                            1001
                    );
                }
            }
        } else {
            Log.e("OpenCV", "OpenCV initialization failed!");
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Error")
                    .setMessage("An error occured. Details: opencv init fail")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        }
    }

    private Sensor rotationVectorSensor;

    @Override
    public void onResume() {
        super.onResume();
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(sensorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // Step 1: Get rotation matrix from rotation vector
                float[] rotationMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

                // Step 2: Remap axes for portrait orientation
                float[] remappedMatrix = new float[9];
                SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,       // Map device X to screen X
                        SensorManager.AXIS_MINUS_Z,  // Map device -Z to screen Y (portrait)
                        remappedMatrix
                );

                // Step 3: Extract pitch from remapped matrix
                float[] orientationAngles = new float[3];
                SensorManager.getOrientation(remappedMatrix, orientationAngles);

                // orientationAngles[1] = pitch (radians)
                devicePitchDegrees = (float) Math.toDegrees(orientationAngles[1]) + 90;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private boolean cameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private static final int INPUT_SIZE = 256; // Model input size

    public class TFLiteHelper {
        private Interpreter tflite;
        private static final String MODEL_FILE = "face_landmarks_detector.tflite";

        public TFLiteHelper(Context context) throws IOException {
            MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_FILE);
            Interpreter.Options options = new Interpreter.Options();
            options.setUseNNAPI(true); // Enable hardware acceleration
            tflite = new Interpreter(modelFile, options);
        }

        public float[][][] detectFace(Bitmap bitmap) {
            // Preprocess input
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
            ByteBuffer inputBuffer = convertBitmapToByteBuffer(scaledBitmap);

            // Run inference
            float[][][][] outputArray = new float[1][1][1][1434]; // [pitch, yaw, roll]
            tflite.run(inputBuffer, outputArray);
            return outputArray[0];
        }

        private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
            buffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            for (int pixel : intValues) {
                buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
                buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
                buffer.putFloat((pixel & 0xFF) / 255.0f);         // B
            }
            return buffer;
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("Camera", "Error", e);
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private TFLiteHelper tfliteHelper;

    @OptIn(markerClass = ExperimentalGetImage.class)
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        try {
            tfliteHelper = new TFLiteHelper(requireContext());
        } catch (IOException e) {
            Log.e("TFLite", "Model loading failed", e);
            return;
        }

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(INPUT_SIZE, INPUT_SIZE))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), imageProxy -> {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap != null) {
                float[][][] angles = tfliteHelper.detectFace(bitmap);

                // Camera matrix approximation
                Mat cameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
                cameraMatrix.put(0, 0, imageProxy.getWidth());
                cameraMatrix.put(1, 1, imageProxy.getWidth());
                cameraMatrix.put(0, 2, imageProxy.getWidth()/2.0);
                cameraMatrix.put(1, 2, imageProxy.getHeight()/2.0);

                Mat rotationVector = new Mat();
                Mat translationVector = new Mat();
                Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
                        new MatOfDouble(), rotationVector, translationVector);

                // Convert rotation vector to Euler angles
                double[] rotationArray = rotationVector.get(0, 0);
                // float headPitch = angles[0]; // Model output order: [pitch, yaw, roll]
                double headPitch = Math.toDegrees(rotationArray[0]);
                float totalPitch = (float) (headPitch + devicePitchDegrees);
                updateUITilt((float) headPitch, totalPitch, devicePitchDegrees);
            }
            imageProxy.close();
        });
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) return null;

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private void updateUITilt(float faceTilt, float deviceTilt) {
        float totalTilt = faceTilt + deviceTilt;
        getActivity().runOnUiThread(() -> {
            faceTiltView.setText(String.format("Neck Tilt: %.1f°", faceTilt));
            finalTiltView.setText(String.format("Final Tilt: %.1f°", totalTilt));
            deviceTiltView.setText(String.format("Device Tilt: %.1f°", deviceTilt));
        });
    }
}