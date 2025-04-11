package com.example.csci3310project.screenTimeTracking.domain;

import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.getAppIcon;
import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.getAppName;
import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.isSystemApp;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.csci3310project.screenTimeTracking.data.AppCategory;
import com.example.csci3310project.screenTimeTracking.data.UsageEntity;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.ui.UsageStatUIModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UsageStatsUseCase {
    private static final String TAG = "UsageStatsUseCase";
    private final UsageRepository usageRepository;
    private final Context context;

    public UsageStatsUseCase(UsageRepository usageRepository, Context context) {
        this.usageRepository = usageRepository;
        this.context = context;
    }

    /**
     * Must be called in a background thread.
     */
    public UsageStatAnalysisReport getUsageStatAnalysisReportSync() {
        List<UsageStats> sortedStats = getProcessUsageStats();
        List<UsageStatAnalysisItem> productivityItems = new ArrayList<>();
        List<UsageStatAnalysisItem> nonProductivityItems = new ArrayList<>();
        long totalProductivityTime = 0;
        long totalNonProductivityTime = 0;
        PackageManager packageManager = context.getPackageManager();

        for (UsageStats stat : sortedStats) {
            try {
                String packageName = stat.getPackageName();
                String appName = getAppName(context, packageName);
                long usage = stat.getTotalTimeInForeground();
                String formattedTime = AppUtils.formatTime(usage);
                UsageEntity appEntity = usageRepository.getAppSync(packageName);
                String appCategory = appEntity != null ? appEntity.app_category : AppCategory.UNCLASSIFIED.getValue();
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                // actually only a few number of apps have description
                CharSequence appDescriptionSequence = applicationInfo.loadDescription(packageManager);
                String appDescription = appDescriptionSequence != null ? appDescriptionSequence.toString() : "";

                if (appCategory.equals(AppCategory.PRODUCTIVE.getValue())) {
                    productivityItems.add(new UsageStatAnalysisItem(appName, formattedTime, appDescription));
                    totalProductivityTime += usage;
                } else if (appCategory.equals(AppCategory.NON_PRODUCTIVE.getValue())) {
                    nonProductivityItems.add(new UsageStatAnalysisItem(appName, formattedTime, appDescription));
                    totalNonProductivityTime += usage;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "getUsageStatAnalysisReportSync: Package not found: " + stat.getPackageName(), e);
            }
        }

        // Since productivityItems and nonProductivityItems are propagated by sortedStats, they are already sorted by usage time in descending order.
        return new UsageStatAnalysisReport(productivityItems, nonProductivityItems, totalProductivityTime, totalNonProductivityTime);
    }

    public LiveData<List<UsageStatUIModel>> getUsageStatsLiveData() {
        // My reference: Day 15 LiveData 介紹與使用 https://ithelp.ithome.com.tw/articles/10222799?sc=rss.iron
        return Transformations.map(usageRepository.getAllAppsLiveData(), usageEntities -> {
            List<UsageStats> rawStats = getProcessUsageStats();
            List<UsageStatUIModel> result = new ArrayList<>();
            for (UsageStats stats : rawStats) {
                String fullPackageName = stats.getPackageName();
                String appName = getAppName(context, fullPackageName);
                Drawable appIcon = getAppIcon(context, fullPackageName);
                UsageEntity appEntity = findUsageEntityByPackageName(usageEntities, fullPackageName);
                String appCategory = appEntity != null ? appEntity.app_category : AppCategory.UNCLASSIFIED.getValue();

                UsageStatUIModel uiModel = new UsageStatUIModel(
                        fullPackageName,
                        appName,
                        stats.getTotalTimeInForeground(),
                        appIcon,
                        appCategory
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

    /**
     * Get the usage stats of all apps that are not system apps and have been used today. The list is sorted by usage time in descending order.
     */
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