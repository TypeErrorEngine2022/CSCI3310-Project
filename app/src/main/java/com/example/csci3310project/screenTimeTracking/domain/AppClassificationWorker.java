package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.csci3310project.screenTimeTracking.data.AppCategory;
import com.example.csci3310project.screenTimeTracking.data.UsageEntity;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;

import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.getAppName;
import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.isSystemApp;

import java.util.List;

public class AppClassificationWorker extends Worker {
    private static final String TAG = "AppClassificationWorker";

    public AppClassificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        LlmInferenceManager llmManager = LlmInferenceManager.getInstance(context);

        try {
            // Wait for llm to be ready
            if (!llmManager.isModelReady()) {
                Log.d(TAG, "LLM model not ready yet, waiting...");
                return Result.retry();
            }

            // Get all installed applications
            PackageManager packageManager = context.getPackageManager();
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            UsageRepository repository = new UsageRepository(getApplicationContext());

            for (ApplicationInfo app : apps) {
                String packageName = app.packageName;
                if (isSystemApp(context, packageName)) {
                    continue;
                }

                String appName = getAppName(context, packageName);

                UsageEntity entity = repository.getAppSync(packageName);

                if (entity == null || entity.app_category.equals(AppCategory.UNCLASSIFIED.getValue())) {
                    classifyApp(llmManager, packageName, appName, repository);
                }
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error classifying apps", e);
            return Result.failure();
        }
    }

    private void classifyApp(LlmInferenceManager llmManager, String packageName, String appName, UsageRepository repository) {
        String prompt = "Is " + appName + " (" + packageName + ") a productivity app? " +
                "Productivity apps help users work, study, or accomplish tasks. " +
                "Answer with 'yes' or 'no' only.";

        llmManager.generateResponseAsync(prompt, new LlmInferenceManager.ResponseCallback() {
            @Override
            public void onResponse(String response) {
                boolean isProductivity = response.trim().toLowerCase().contains("yes");
                Log.d(TAG, "App: " + appName + " classified as: " + (AppCategory.fromBoolean(isProductivity)));
                repository.updateAppProductivityAsync(packageName, isProductivity);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error classifying " + appName + ": " + errorMessage);
            }
        });
    }
}