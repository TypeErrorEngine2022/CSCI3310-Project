package com.example.csci3310project.screenTimeTracking.domain;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.ui.UsageStatUIModel;

import java.util.List;
import java.util.stream.Collectors;

public class UsageStatsUseCase {
    UsageRepository usageRepository;
    Context context;

    public UsageStatsUseCase(UsageRepository usageRepository, Context context) {
        this.usageRepository = usageRepository;
        this.context = context;
    }

    // Reference: Android应用层PackageManager的使用 https://www.cnblogs.com/dony-c/p/9478115.html
    private String getAppName(UsageStats entity) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(entity.getPackageName(), PackageManager.GET_META_DATA)
            ).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UsageStatsUseCase", "Package not found: " + entity.getPackageName(), e);
            return entity.getPackageName(); // Fallback to package name if app name is not found
        }
    }

    // reference: https://stackoverflow.com/questions/17985500/how-can-i-get-the-applications-icon-from-the-package-name
    private Drawable getAppIcon(UsageStats stat) {
        try {
            return context.getPackageManager().getApplicationIcon(stat.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UsageStatsUseCase", "Package not found: " + stat.getPackageName(), e);
        }
        return null;
    }

    public List<UsageStatUIModel> getDailyUsageStats() {
        List<UsageStats> rawStats = usageRepository.getDailyUsageStats();
        rawStats = rawStats.stream().filter(usageStats -> usageStats.getTotalTimeInForeground() > 0).collect(Collectors.toList());
        rawStats.sort((o1, o2) -> Long.compare(o2.getTotalTimeInForeground(), o1.getTotalTimeInForeground()));
        return rawStats.stream().map(usageStats -> {
            String appName = getAppName(usageStats);
            Drawable appIcon = getAppIcon(usageStats);
            return new UsageStatUIModel(
                    appName,
                    usageStats.getTotalTimeInForeground(),
                    appIcon
            );
        }
        ).collect(Collectors.toList());
    }
}
