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
import android.os.SystemClock;
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
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.MediaImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.*;

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;


public class Page3AFragment extends Fragment {
    // UI Elements
    private PreviewView previewView;
    private TextView faceTiltView, finalTiltView, deviceTiltView;

    // Camera/Sensor
    private ProcessCameraProvider cameraProvider;
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

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

            if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
                // Device doesn't support rotation vector sensor
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Feature Not Available")
                        .setMessage("Your device doesn't support the required sensors for this feature.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            } else {
                initializeMediaPipe();
                // Check camera permissions before starting if not granted request them
                if (cameraPermission()) {
                    startCamera();
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 1001);
                }
            }
    }

    private void initializeMediaPipe() {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build();

        FaceLandmarker.FaceLandmarkerOptions options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::processLandmarkerResults)
                .build();

        faceLandmarker = FaceLandmarker.createFromOptions(requireContext(), options);
    }

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

    @OptIn(markerClass = ExperimentalGetImage.class)
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), imageProxy -> {
            long frameTime = SystemClock.uptimeMillis();

            // Convert ImageProxy to MPImage
            MPImage mpImage = imageProxyToMPImage(imageProxy);
            if (mpImage != null) {
                faceLandmarker.detectAsync(mpImage, frameTime);
            }

            imageProxy.close();
        });

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
    }

    private void processLandmarkerResults(FaceLandmarkerResult result, MPImage mpImage) {
        if (result.faceLandmarks().isEmpty()) return;

        int[] landmarkIndices = {4, 199, 33, 263, 61, 291};
        List<Point> points = new ArrayList<>();

        for (int index : landmarkIndices) {
            NormalizedLandmark landmark = result.faceLandmarks().get(0).get(index);
            // Flip x-coordinate for front-facing camera
            float flippedX = 1.0f - landmark.x();
            points.add(new Point(
                    flippedX * mpImage.getWidth(),
                    landmark.y() * mpImage.getHeight()
            ));
        }

        MatOfPoint2f imagePoints = new MatOfPoint2f();
        imagePoints.fromList(points);

        Mat cameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
        double focalLength = mpImage.getWidth();
        cameraMatrix.put(0, 0, focalLength);
        cameraMatrix.put(1, 1, focalLength);
        cameraMatrix.put(0, 2, mpImage.getWidth() / 2.0);
        cameraMatrix.put(1, 2, mpImage.getHeight() / 2.0);

        // Solve PnP
        Mat rotationVector = new Mat();
        Mat translationVector = new Mat();
        Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
                new MatOfDouble(), rotationVector, translationVector);

        // Convert to Euler angles
        Mat rotationMatrix = new Mat();
        Calib3d.Rodrigues(rotationVector, rotationMatrix);
        double pitch = Math.toDegrees(Math.asin(-rotationMatrix.get(2, 1)[0]));

        if (devicePitchDegrees > 70) {

        } else if (devicePitchDegrees > 60) {
            pitch += 5;
        } else if (devicePitchDegrees > 55) {
            pitch += 10;
        } else if (devicePitchDegrees > 50) {
            pitch += 15;
        } else if (devicePitchDegrees > 35) {
            pitch += 20;
        } else {
            pitch += 25;
        }
        updateUI((float) pitch, devicePitchDegrees);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private MPImage imageProxyToMPImage(ImageProxy imageProxy) {
        android.media.Image image = imageProxy.getImage();
        if (image == null) return null;
        return new MediaImageBuilder(image).build();
    }

    private void updateUI(float faceTilt, float deviceTilt) {

        float totalTilt = faceTilt + deviceTilt;
        getActivity().runOnUiThread(() -> {
            faceTiltView.setText(String.format("Neck Tilt: %.1f°", faceTilt));
            finalTiltView.setText(String.format("Final Tilt: %.1f°", totalTilt));
            deviceTiltView.setText(String.format("Device Tilt: %.1f°", deviceTilt));
        });
    }
}