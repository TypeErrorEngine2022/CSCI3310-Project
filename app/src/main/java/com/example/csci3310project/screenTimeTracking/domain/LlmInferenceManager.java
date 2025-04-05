package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// Reference: 管理工作 https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work?hl=zh-tw
// In our app, the LLM model is initialized in a background thread using WorkManager.
// The LLMInferenceManager class manages the initialization and inference of the LLM model.
// There will be a lot of inference request during the first launch of the app, because we need to classify all installed apps.
// So, we need work manager to manage the inference requests.

public class LlmInferenceManager {
    private static final String TAG = "LlmInferenceManager";
    private static final String INIT_WORK_NAME = "llm_initialization_work";
    private static LlmInferenceManager instance;

    private final Context appContext;
    private final MutableLiveData<Boolean> modelReadyState = new MutableLiveData<>(false);

    // Thread-safe map to store callbacks for each inference request
    private final ConcurrentHashMap<UUID, ResponseCallback> callbackMap = new ConcurrentHashMap<>();

    private LlmInferenceManager(Context context) {
        this.appContext = context.getApplicationContext();
        initializeModel();
    }

    public static synchronized LlmInferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new LlmInferenceManager(context);
        }
        return instance;
    }

    public Context getAppContext() {
        return appContext;
    }

    public LiveData<Boolean> getModelReadyState() {
        return modelReadyState;
    }

    private void initializeModel() {
        // since llm initialization will take less than 10 minutes, use one time work request
        // in my samsung s23, it takes about 10 seconds to initialize the model
        // in case the user quit the app during initialization, we need to use work manager
        OneTimeWorkRequest initRequest = new OneTimeWorkRequest.Builder(LlmInitializationWorker.class)
                .addTag(INIT_WORK_NAME)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        10000,
                        TimeUnit.MILLISECONDS)
                .build();

        // Enqueue the initialization work, do not replace existing work
        WorkManager.getInstance(appContext)
                .enqueueUniqueWork(
                        INIT_WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        initRequest);

        WorkManager.getInstance(appContext).getWorkInfoByIdLiveData(initRequest.getId())
                .observeForever(workInfo -> {
                    if (workInfo != null) {
                        boolean isSuccess = workInfo.getState() == WorkInfo.State.SUCCEEDED;
                        modelReadyState.postValue(isSuccess);
                        Log.d(TAG, "Model initialization " + (isSuccess ? "succeeded" : "failed"));
                    }
                });
    }

    public void generateResponseAsync(String prompt, ResponseCallback callback) {
        if (!Boolean.TRUE.equals(modelReadyState.getValue())) {
            callback.onError("Model not initialized yet");
            return;
        }

        Data inputData = new Data.Builder()
                .putString(LlmInferenceWorker.KEY_PROMPT, prompt)
                .build();

        OneTimeWorkRequest inferenceRequest = new OneTimeWorkRequest.Builder(LlmInferenceWorker.class)
                .setInputData(inputData)
                .build();

        // Store the callback to retrieve it when work is complete
        callbackMap.put(inferenceRequest.getId(), callback);

        // Observe work result to deliver back to callback
        // each generateResponseAsync will create a new work request and observe that request
        WorkManager.getInstance(appContext).getWorkInfoByIdLiveData(inferenceRequest.getId())
                .observeForever(workInfo -> {
                    if (workInfo == null) return;

                    if (workInfo.getState().isFinished()) {
                        ResponseCallback storedCallback = callbackMap.remove(inferenceRequest.getId());
                        if (storedCallback != null) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String result = workInfo.getOutputData().getString(LlmInferenceWorker.KEY_RESULT);
                                storedCallback.onResponse(result != null ? result : "No response generated");
                            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                String error = workInfo.getOutputData().getString(LlmInferenceWorker.KEY_ERROR);
                                storedCallback.onError(error != null ? error : "Unknown error");
                            }
                        }
                    }
                });

        WorkManager.getInstance(appContext).enqueue(inferenceRequest);
    }

    public boolean isModelReady() {
        return Boolean.TRUE.equals(modelReadyState.getValue());
    }

    public interface ResponseCallback {
        void onResponse(String response);
        void onError(String errorMessage);
    }
}