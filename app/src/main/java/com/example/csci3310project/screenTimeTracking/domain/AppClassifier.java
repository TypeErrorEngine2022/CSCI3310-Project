package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.util.Log;

import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.data.UsageStatsDataSource;
import com.example.csci3310project.screenTimeTracking.ui.UsageStatUIModel;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import java.util.List;

// Reference: LLM Inference guide for Android https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
// Reference: AI Edge RAG guide for Android https://ai.google.dev/edge/mediapipe/solutions/genai/rag/android
public class AppClassifier {
    private static LlmInference llmInference;
    private static final String MODEL_PATH = "/data/local/tmp/llm/gemma3.task";

    public static void initialize(Context context) {
        try {
            LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(MODEL_PATH)
                    .setMaxTokens(512)
                    .setMaxTopK(40)
                    .setPreferredBackend(LlmInference.Backend.GPU)
                    .build();
            llmInference = LlmInference.createFromOptions(context, options);
        } catch (Exception e) {
            Log.e("AppClassifier", "Error initializing LLM model", e);
        }
    }

    // Classify an app as productive or non-productive
    public static boolean classifyApp(String appName) {
        if (llmInference == null) {
            Log.e("AppClassifier", "LLM model not initialized");
            return false;
        }
        String prompt = "Is " + appName + " a productivity app? Answer 'yes' or 'no' only.";
        String response = llmInference.generateResponse(prompt);
        return response.trim().equalsIgnoreCase("yes");
    }


    // Generate a comment based on usage data in English
    // if we use Cantonese, the response is not good
    public static String generateComment(Context context) {
        if (llmInference == null) {
            Log.e("AppClassifier", "LLM model not initialized");
            return "Unable to generate comment, please check model settings.";
        }
        StringBuilder prompt = new StringBuilder("Assume the user's current usage time in the format [app, hours] is as follows:\n{");
        UsageStatsUseCase usageStatsUseCase = new UsageStatsUseCase(new UsageRepository(new UsageStatsDataSource(context)), context);
        List<UsageStatUIModel> usageStatsData = usageStatsUseCase.getDailyUsageStats();
        for (UsageStatUIModel usageStat : usageStatsData) {
            long hours = usageStat.getTotalMsInForeground() / 3600000;  // Convert ms to hours
            prompt.append(usageStat.getPackageName()).append(" ").append(hours).append(",");
        }
        prompt.setLength(prompt.length() - 1);  // Remove last comma
        prompt.append("}\nGenerate a comment about the user's productivity. If the user can be more efficient, use positive encouragement; otherwise, encourage the user. The comment should be less than 100 words.");
        Log.d("AppClassifier", "Prompt: " + prompt);
        return llmInference.generateResponse(prompt.toString());
    }
}