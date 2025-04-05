package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class LlmInferenceManager {
    private static final String MODEL_PATH = "/data/local/tmp/llm/gemma3.task";
    private static final String TAG = "LlmInferenceManager";
    private static LlmInferenceManager instance;
    
    private LlmInference llmInference;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Reference: 并发编程 — AtomicBoolean 详解 https://blog.csdn.net/small_love/article/details/111057268
    private final AtomicBoolean isInitializing = new AtomicBoolean(false);

    // Reference: Android开发 LiveData与MutableLiveData详解  https://www.cnblogs.com/guanxinjing/p/11544273.html
    private final MutableLiveData<Boolean> modelReadyState = new MutableLiveData<>(false);
    
    private LlmInferenceManager(Context context) {
        // Use application context to avoid leaks
        // if not using this, the app will crash when the activity is destroyed
        initialize(context.getApplicationContext()); 
    }

    // use synchronized to ensure thread safety
    public static synchronized LlmInferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new LlmInferenceManager(context);
        }
        return instance;
    }
    
    public LiveData<Boolean> getModelReadyState() {
        return modelReadyState;
    }

    /**
     * Initialize the LLM model in a background thread. This method is thread-safe.
     */
    private void initialize(Context context) {
        if (isInitializing.getAndSet(true)) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting LLM model initialization");
                LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(MODEL_PATH)
                        .setPreferredBackend(LlmInference.Backend.GPU)
                        .build();
                llmInference = LlmInference.createFromOptions(context, options);
                modelReadyState.postValue(true);
                Log.d(TAG, "LLM model initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing LLM model", e);
                modelReadyState.postValue(false);
            } finally {
                isInitializing.set(false);
            }
        });
    }

    /**
     * Generate a response from the LLM model asynchronously.
     * @param prompt The input prompt for the model.
     * @param callback  The callback to handle the response or error.
     */
    public void generateResponseAsync(String prompt, ResponseCallback callback) {
        if (llmInference == null) {
            callback.onError("Model not initialized yet");
            return;
        }
        
        executorService.submit(() -> {
            try {
                String response = llmInference.generateResponse(prompt);
                callback.onResponse(response);
            } catch (Exception e) {
                Log.e(TAG, "Error generating response", e);
                callback.onError("Failed to generate response: " + e.getMessage());
            }
        });
    }
    
    // Non-blocking check if model is ready
    public boolean isModelReady() {
        return llmInference != null && Boolean.TRUE.equals(modelReadyState.getValue());
    }
    
    public LlmInference getLlmInference() {
        return llmInference;
    }
    
    public interface ResponseCallback {
        void onResponse(String response);
        void onError(String errorMessage);
    }
}