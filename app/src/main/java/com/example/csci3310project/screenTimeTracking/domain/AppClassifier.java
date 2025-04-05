package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.util.Log;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

public class AppClassifier {
    private LlmInference llmInference;

    public AppClassifier(Context context) {
        LlmInferenceManager llmInferenceManager = LlmInferenceManager.getInstance(context);
        while ((this.llmInference = llmInferenceManager.getLlmInference()) == null) {
            try {
                Thread.sleep(100); // Wait for 100ms before retrying
            } catch (InterruptedException e) {
                Log.e("AppClassifier", "Initialization interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public boolean classifyApp(String appName) {
        if (llmInference == null) {
            Log.e("AppClassifier", "LLM model not initialized");
            return false;
        }
        String prompt = "Is " + appName + " a productivity app? Answer 'yes' or 'no' only.";
        String response = llmInference.generateResponse(prompt);
        return response.trim().equalsIgnoreCase("yes");
    }
}