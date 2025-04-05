package com.example.csci3310project;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;

public class Page3AFragment extends Fragment {
    PreviewView previewView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.page3a, container, false);
    }

    private FaceDetector faceDetector;
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewView);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);


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
            // Check camera permissions before starting
            // If not granted request them
            if (cameraPermission()) {
                startCamera();
            } else {
                requestPermissions(
                        new String[]{Manifest.permission.CAMERA},
                        1001
                );
            }
        }
    }

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private volatile float devicePitchDegrees = 0.0f;

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
        return ContextCompat.checkSelfPermission(getContext(), "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED;
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

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getContext()), imageProxy -> {
            InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        if (!faces.isEmpty()) {
                            Face face = faces.get(0);
                            float faceAngleX = face.getHeadEulerAngleX();
                            float totalTilt = devicePitchDegrees + faceAngleX;
                            updateUITilt(faceAngleX);
                            updateUITilt2(totalTilt);
                            updateUITilt3(devicePitchDegrees);
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> imageProxy.close());


        });

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
    }

    private void updateUITilt(float faceAngleX) {
        getActivity().runOnUiThread(() -> {
            TextView tiltView = getView().findViewById(R.id.faceAngle);
            tiltView.setText(String.format("Neck Tilt: %.1f°", faceAngleX));
        });
    }
    private void updateUITilt2(float totalTilt) {
        getActivity().runOnUiThread(() -> {
            TextView tiltView = getView().findViewById(R.id.finalAngle);
            tiltView.setText(String.format("Final Tilt: %.1f°", totalTilt));
        });
    }
    private void updateUITilt3(float deviceTilt) {
        getActivity().runOnUiThread(() -> {
            TextView tiltView = getView().findViewById(R.id.deviceAngle);
            tiltView.setText(String.format("Device Tilt: %.1f°", deviceTilt));
        });
    }
}