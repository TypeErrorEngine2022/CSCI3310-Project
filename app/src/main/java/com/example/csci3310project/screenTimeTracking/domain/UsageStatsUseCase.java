package com.example.csci3310project.screenTimeTracking.domain;

import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.getAppIcon;
import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.getAppName;
import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.isSystemApp;

import android.app.usage.UsageStats;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.csci3310project.screenTimeTracking.data.UsageEntity;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.ui.UsageStatUIModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class UsageStatsUseCase {
    private final UsageRepository usageRepository;
    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public UsageStatsUseCase(UsageRepository usageRepository, Context context) {
        this.usageRepository = usageRepository;
        this.context = context;
    }

    public List<UsageStatUIModel> getUsageStatsSync() {
        List<UsageStats> rawStats = getProcessUsageStats();
        List<UsageStatUIModel> result = new ArrayList<>();
        for (UsageStats stats : rawStats) {
            String fullPackageName = stats.getPackageName();
            String appName = getAppName(context, fullPackageName);
            Drawable appIcon = getAppIcon(context, fullPackageName);
            UsageEntity appEntity = usageRepository.getAppSync(fullPackageName);
            boolean isProductive = appEntity != null && appEntity.isProductiveApp();

            UsageStatUIModel uiModel = new UsageStatUIModel(
                    appName,
                    stats.getTotalTimeInForeground(),
                    appIcon,
                    isProductive
            );
            result.add(uiModel);
        }

        return result;
    }

    public LiveData<List<UsageStatUIModel>> getUsageStatsLiveData() {
        return Transformations.map(usageRepository.getAllAppsLiveData(), usageEntities -> {
            List<UsageStats> rawStats = getProcessUsageStats();
            List<UsageStatUIModel> result = new ArrayList<>();
            for (UsageStats stats : rawStats) {
                String fullPackageName = stats.getPackageName();
                String appName = getAppName(context, fullPackageName);
                Drawable appIcon = getAppIcon(context, fullPackageName);
                UsageEntity appEntity = findUsageEntityByPackageName(usageEntities, fullPackageName);
                boolean isProductive = appEntity != null && appEntity.isProductiveApp();

                UsageStatUIModel uiModel = new UsageStatUIModel(
                        appName,
                        stats.getTotalTimeInForeground(),
                        appIcon,
                        isProductive
                );
                result.add(uiModel);
            }
            return result;
        });
    }

    private UsageEntity findUsageEntityByPackageName(List<UsageEntity> usageEntities, String packageName) {
        for (UsageEntity entity : usageEntities) {
            if (entity.packageName.equals(packageName)) {
                return entity;
            }
        }
        return null;
    }

    private List<UsageStats> getProcessUsageStats() {
        List<UsageStats> rawStats = usageRepository.getDailyUsageStats();
        rawStats = rawStats.stream()
                .filter(usageStats -> usageStats.getTotalTimeInForeground() > 0 && !isSystemApp(context, usageStats.getPackageName()))
                .collect(Collectors.toList());

        // Sort by usage time in descending order
        rawStats.sort((o1, o2) -> Long.compare(o2.getTotalTimeInForeground(), o1.getTotalTimeInForeground()));
        return rawStats;
    }
}