package com.example.csci3310project.screenTimeTracking.data;

import android.app.usage.UsageStats;

import java.util.List;

public class UsageRepository {
    UsageStatsDataSource usageStatsDataSource;
    UsageDao usageDao;

    public UsageRepository(UsageStatsDataSource usageStatsDataSource) {
        this.usageStatsDataSource = usageStatsDataSource;
    }

    public List<UsageStats> getDailyUsageStats() {
        return usageStatsDataSource.getDailyUsageStats();
    }
}
