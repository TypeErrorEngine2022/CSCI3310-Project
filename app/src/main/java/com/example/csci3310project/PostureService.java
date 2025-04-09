package com.example.csci3310project;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleService;

import com.example.csci3310project.neckPostureDatabase.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.MediaImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PostureService extends LifecycleService {

    private ScheduledExecutorService scheduler;
    private int consecutiveBadPosture = 0;
    private double threshold;
    private long interval;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private float devicePitchDegrees = 0;

    @Override
    public void onCreate() {
        Log.d("My_debug","Posture Service java onCreate");
        super.onCreate();
        // Start foreground notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "posture_channel",
                    "Posture Analysis",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, "posture_channel")
                .setContentTitle("Posture Analysis")
                .setContentText("Running in background")
                .setSmallIcon(R.drawable.face_shake_24px)
                .build();
        startForeground(1, notification);
        Log.d("My_debug", "running in background notification sent");

        // Initialize camera
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                setupCamera();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("PostureService", "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(sensorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private UserSettings getSettingsFromDatabase() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        return db.userSettingsDao().getSettingsSync();
    }
    private ImageAnalysis imageAnalysis;
    private long lastProcessedTime = 0;
    private void setupCamera() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();
        imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @OptIn(markerClass = ExperimentalGetImage.class)
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastProcessedTime >= interval) {
                    lastProcessedTime = currentTime;
                    MPImage mpImage = new MediaImageBuilder(imageProxy.getImage()).build();
                    double faceTilt = calculateFaceTilt(mpImage);
                    Log.d("My_debug", "Image processed");
                    double totalTilt = faceTilt + devicePitchDegrees;
                    Log.d("My_debug", "total tilt = " + totalTilt + ", faceTilt = " + faceTilt + ", devicePitchDegrees = " + devicePitchDegrees);
                    if (totalTilt < threshold) {
                        consecutiveBadPosture++;
                        Log.d("My_debug", "consecutiveBadPosture = " + consecutiveBadPosture + " / " + 30/(interval / 1000));
                        if (consecutiveBadPosture * (interval / 1000) >= 30 && getSettingsFromDatabase().notificationsEnabled) {
                            Log.d("My_debug", "bad posture breakpoint reached, trying to trigger notification");
                            sendNotification();
                            consecutiveBadPosture = 0;
                        }
                    } else {
                        Log.d("My_debug", "consecutive bad posture reset");
                        consecutiveBadPosture = 0;
                    }
                }
                imageProxy.close();
            }
        });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//        UserSettings settings = getSettingsFromDatabase();
//        if (settings != null && settings.postureAnalysisEnabled) {
//            interval = parseInterval(settings.checkInterval);
//            threshold = getThreshold(settings.toleranceLevel);
//        }
//        if (intent != null && "UPDATE_SETTINGS".equals(intent.getAction())) {
//            UserSettings updatedSettings = getSettingsFromDatabase();
//            if (updatedSettings != null) {
//                interval = parseInterval(updatedSettings.checkInterval);
//                threshold = getThreshold(updatedSettings.toleranceLevel);
//            }
//        }
        if (intent != null && "UPDATE_SETTINGS".equals(intent.getAction())) {
            UserSettings settings = getSettingsFromDatabase();
            if (settings != null) {
                this.interval = parseInterval(settings.checkInterval);
                this.threshold = getThreshold(settings.toleranceLevel);
                // Optionally reset consecutiveBadPosture
                this.consecutiveBadPosture = 0;
            }
        } else {
            // Initial start
            UserSettings settings = getSettingsFromDatabase();
            if (settings != null && settings.postureAnalysisEnabled) {
                this.interval = parseInterval(settings.checkInterval);
                this.threshold = getThreshold(settings.toleranceLevel);
            }
        }
        return START_STICKY;
    }

//    private void checkPosture() {
//        // Take picture and process
//        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
//            @OptIn(markerClass = ExperimentalGetImage.class)
//            @Override
//            public void onCaptureSuccess(@NonNull ImageProxy image) {
//                MPImage mpImage = new MediaImageBuilder(image.getImage()).build();
//                double faceTilt = calculateFaceTilt(mpImage);
//                double totalTilt = faceTilt + devicePitchDegrees;
//
//                if (totalTilt > threshold) {
//                    consecutiveBadPosture++;
//                    if (consecutiveBadPosture * (interval / 1000) >= 60 && getSettingsFromDatabase().notificationsEnabled) {
//                        sendNotification();
//                        consecutiveBadPosture = 0;
//                    }
//                } else {
//                    consecutiveBadPosture = 0;
//                }
//                image.close();
//            }
//
//            @Override
//            public void onError(@NonNull ImageCaptureException exception) {
//                Log.e("PostureService", "Error capturing image", exception);
//            }
//        });
//    }

    private FaceLandmarker faceLandmarker;
    private double calculateFaceTilt(MPImage mpImage) {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build();

        FaceLandmarker.FaceLandmarkerOptions options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .build();

        faceLandmarker = FaceLandmarker.createFromOptions(this, options);
        FaceLandmarkerResult result = faceLandmarker.detect(mpImage);

        if (result.faceLandmarks().isEmpty()) return 0;
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
        final MatOfPoint3f objectPoints = new MatOfPoint3f(
                new Point3(0.0f, 0.0f, 0.0f),           // Nose tip
                new Point3(0.0f, -330.0f, -65.0f),      // Chin
                new Point3(-225.0f, 170.0f, -135.0f),   // Left eye left corner
                new Point3(225.0f, 170.0f, -135.0f),    // Right eye right corner
                new Point3(-150.0f, -150.0f, -125.0f),  // Left mouth corner
                new Point3(150.0f, -150.0f, -125.0f)    // Right mouth corner
        );
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
        return pitch;
    }

    private long parseInterval(String intervalStr) {
        switch (intervalStr) {
            case "Real-time": return 500;
            case "5 seconds": return 5000;
            case "10 seconds": return 10000;
            case "15 seconds": return 15000;
            case "30 seconds": return 30000;
            case "1 minute": return 60000;
            default: return 5000;
        }
    }

    private double getThreshold(String toleranceLevel) {
        switch (toleranceLevel) {
            case "Low": return 80;
            case "Medium": return 73;
            case "High": return 65;
            default: return 73;
        }
    }

    private void sendNotification() {
        Log.d("My_debug", "sendNotification() triggered");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "posture_channel")
                .setSmallIcon(R.drawable.face_up_24px)
                .setContentTitle("Bad Posture Detected")
                .setContentText("Please adjust your head position for better posture.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(2, builder.build());
            Log.d("My_debug", "bad posture detected notification sent");
        }

    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                float[] remappedMatrix = new float[9];
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, remappedMatrix);
                float[] orientationAngles = new float[3];
                SensorManager.getOrientation(remappedMatrix, orientationAngles);
                devicePitchDegrees = (float) Math.toDegrees(orientationAngles[1]) + 90;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) scheduler.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (sensorManager != null && rotationVectorSensor != null) {
            sensorManager.unregisterListener(sensorListener, rotationVectorSensor);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}