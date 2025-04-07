package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LlmInitializationWorker extends Worker {
    private static final String TAG = "LlmInitializationWorker";
    private static final String MODEL_FILENAME = "gemma3.task";

    // under src/main/assets
    private static final String MODEL_ASSET_PATH = "models/gemma3-1b-it-int4.task";

    private static volatile String modelPath;

    // Use volatile to ensure visibility across threads
    private static volatile LlmInference llmInference;

    public LlmInitializationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting LLM model initialization");

            getModelPath(getApplicationContext());

            if (modelPath == null) {
                return Result.failure();
            }

            LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setPreferredBackend(LlmInference.Backend.GPU)
                    .build();

            llmInference = LlmInference.createFromOptions(getApplicationContext(), options);
            Log.d(TAG, "LLM model initialized successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing LLM model", e);
            return Result.retry();
        }
    }

    /**
     * Extract the model from assets to the app's internal storage.
     * MediaPipe cannot take asset path, so we need to extract it first.
     * Although this will cause 2 copies of model, but keep it simple for now.
     */
    private void getModelPath(Context context) {
        // it should be stored in /data/data/com.example.csci3310project/files/gemma3.task
        // in my samsung s23, after extraction, the app size is 1.5GB
        // if i clear cache, the model will be deleted, the app size will be 0.99GB
        // but the model will be extracted again when the app is launched
        File modelFile = new File(context.getFilesDir(), MODEL_FILENAME);
        modelPath = modelFile.getAbsolutePath();

        // Check if model already exists and is valid
        if (modelFile.exists() && modelFile.length() > 0) {
            Log.d(TAG, "Model already exists at: " + modelPath);
            return;
        }

        long requiredSpace = estimateModelSize();
        if (context.getFilesDir().getFreeSpace() < requiredSpace) {
            Log.e(TAG, "Not enough space to extract model. Required: " + (requiredSpace / (1024 * 1024)) + " MB");
            modelPath = null;
            return;
        }

        Log.d(TAG, "Extracting model from assets to: " + modelPath);
        long startTime = System.currentTimeMillis();

        try (InputStream is = context.getAssets().open(MODEL_ASSET_PATH);
             OutputStream os = new FileOutputStream(modelFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            long fileSize = is.available();

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);

                totalBytesRead += bytesRead;
                // Log every 10MB
                // I need to check extract time during testing
                // In my samsung s23, it takes about 3 seconds to extract 555MB model
                if (totalBytesRead % (10 * 1024 * 1024) == 0) {
                    Log.d(TAG, String.format("Extracted %d MB / %d MB (%.1f%%)",
                            totalBytesRead / (1024 * 1024),
                            fileSize / (1024 * 1024),
                            (totalBytesRead * 100f) / fileSize));
                }
            }

            long endTime = System.currentTimeMillis();
            Log.d(TAG, "Model extracted successfully in " + (endTime - startTime) / 1000 + " seconds");
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract model from assets", e);
            // really need this, if not, you will feel the pain of storage problem during testing
            if (modelFile.exists()) {
                boolean deleted = modelFile.delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete partial model file: " + modelFile.getAbsolutePath());
                }
            }
            modelPath = null;
        }
    }

    private long estimateModelSize() {
        return 700 * 1024 * 1024L; // 700MB
    }

    // Singleton pattern accessor
    public static LlmInference getLlmInference() {
        return llmInference;
    }
}