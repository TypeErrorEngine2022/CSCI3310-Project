package com.example.csci3310project.screenTimeTracking.domain;

import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.getAppIcon;
import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.getAppName;
import static com.example.csci3310project.screenTimeTracking.domain.AppUtils.isSystemApp;

import android.app.usage.UsageStats;
import android.content.Context;
import android.graphics.drawable.Drawable;

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

    public List<UsageStatUIModel> getDailyUsageStats() {
        List<UsageStats> rawStats = usageRepository.getDailyUsageStats();
        rawStats = rawStats.stream().filter(usageStats -> usageStats.getTotalTimeInForeground() > 0).collect(Collectors.toList());
        rawStats.sort((o1, o2) -> Long.compare(o2.getTotalTimeInForeground(), o1.getTotalTimeInForeground()));
        return rawStats.stream().
                filter(usageStats -> !isSystemApp(context, usageStats.getPackageName())) // Filter out system apps
                .map(usageStats -> { // map UsageStats to UsageStatUIModel
                            String appName = getAppName(context, usageStats.getPackageName());
                            Drawable appIcon = getAppIcon(context, usageStats.getPackageName());
                            return new UsageStatUIModel(
                                    appName,
                                    usageStats.getTotalTimeInForeground(),
                                    appIcon
                            );
                        }
                ).collect(Collectors.toList());
    }
}
