package com.example.csci3310project.screenTimeTracking.domain;

import android.app.usage.UsageStats;

import com.example.csci3310project.screenTimeTracking.data.UsageEntity;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;

import java.util.List;
import java.util.stream.Collectors;

public class UsageStatsUseCase {
    UsageRepository usageRepository;

    public UsageStatsUseCase(UsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    public List<UsageEntity> getDailyUsageStats() {
        List<UsageStats> rawStats = usageRepository.getDailyUsageStats();
        rawStats = rawStats.stream().filter(usageStats -> usageStats.getTotalTimeInForeground() > 0).collect(Collectors.toList());
        rawStats.sort((o1, o2) -> Long.compare(o2.getTotalTimeInForeground(), o1.getTotalTimeInForeground()));
        return rawStats.stream().map(usageStats -> new UsageEntity(usageStats.getPackageName(), usageStats.getTotalTimeInForeground())).collect(Collectors.toList());
    }
}
