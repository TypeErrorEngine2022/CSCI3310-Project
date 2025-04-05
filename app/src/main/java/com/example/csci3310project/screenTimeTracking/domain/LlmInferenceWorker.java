package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

public class LlmInferenceWorker extends Worker {
    private static final String TAG = "LlmInferenceWorker";
    public static final String KEY_PROMPT = "prompt";
    public static final String KEY_RESULT = "result";
    public static final String KEY_ERROR = "error";

    public LlmInferenceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String prompt = getInputData().getString(KEY_PROMPT);
        if (prompt == null || prompt.isEmpty()) {
            return createFailureResult("Empty prompt provided");
        }

        LlmInference llm = LlmInitializationWorker.getLlmInference();
        if (llm == null) {
            return createFailureResult("LLM model not initialized");
        }

        try {
            String response = llm.generateResponse(prompt);
            Data outputData = new Data.Builder()
                    .putString(KEY_RESULT, response)
                    .build();
            return Result.success(outputData);
        } catch (Exception e) {
            Log.e(TAG, "Error generating response", e);
            return createFailureResult("Failed to generate response: " + e.getMessage());
        }
    }

    private Result createFailureResult(String errorMessage) {
        Data outputData = new Data.Builder()
                .putString(KEY_ERROR, errorMessage)
                .build();
        return Result.failure(outputData);
    }
}