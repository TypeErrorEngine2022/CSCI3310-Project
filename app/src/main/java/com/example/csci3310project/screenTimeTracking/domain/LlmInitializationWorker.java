package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

public class LlmInitializationWorker extends Worker {
    private static final String TAG = "LlmInitializationWorker";
    private static final String MODEL_PATH = "/data/local/tmp/llm/gemma3.task";

    // use volatile to ensure visibility across threads
    private static volatile LlmInference llmInference;

    public LlmInitializationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting LLM model initialization");

            LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(MODEL_PATH)
                    .setPreferredBackend(LlmInference.Backend.GPU)
                    .build();

            llmInference = LlmInference.createFromOptions(getApplicationContext(), options);
            Log.d(TAG, "LLM model initialized successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing LLM model", e);
            return Result.failure();
        }
    }

    // let's try singleton pattern in this project
    public static LlmInference getLlmInference() {
        return llmInference;
    }
}
