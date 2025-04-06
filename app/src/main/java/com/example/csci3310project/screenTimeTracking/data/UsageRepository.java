package com.example.csci3310project.screenTimeTracking.data;

import android.app.usage.UsageStats;
import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

public class UsageRepository {
    private final AppDatabase appDatabase;

    private final UsageStatsDataSource usageStatsDataSource;

    public UsageRepository(Context context) {
        this.appDatabase = AppDatabase.getDatabase(context);
        this.usageStatsDataSource = new UsageStatsDataSource(context);
    }

    public List<UsageStats> getDailyUsageStats() {
        return usageStatsDataSource.getDailyUsageStats();
    }

    public LiveData<List<UsageEntity>> getAllAppsLiveData() {
        return appDatabase.usageDao().getAll();
    }

    /**
     * Must be called in a background thread.
     */
    public UsageEntity getAppSync(String packageName) {
        return appDatabase.usageDao().findByPackageNameSync(packageName);
    }

    public void updateAppProductivityAsync(String fullPackageName, boolean isProductive) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            updateAppProductivitySync(fullPackageName, isProductive);
        });
    }


    /**
     * Must be called in a background thread.
     */
    private void updateAppProductivitySync(String fullPackageName, boolean isProductive) {
        UsageEntity app = appDatabase.usageDao().findByPackageNameSync(fullPackageName);
        String category = AppCategory.fromBoolean(isProductive);
        if (app != null) {
            appDatabase.usageDao().updateAppCategory(fullPackageName, category);
        } else {
            UsageEntity newEntity = new UsageEntity();
            newEntity.packageName = fullPackageName;
            newEntity.app_category = category;
            appDatabase.usageDao().insertUsageEntity(newEntity);
        }
    }
}