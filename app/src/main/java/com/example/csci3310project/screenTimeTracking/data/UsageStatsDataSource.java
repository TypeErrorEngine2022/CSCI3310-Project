package com.example.csci3310project.screenTimeTracking.data;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class UsageStatsDataSource {
    private final UsageStatsManager usageStatsManager;

    public UsageStatsDataSource(Context context) {
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    // My Reference: https://stackoverflow.com/questions/22990067/how-to-extract-epoch-from-localdate-and-localdatetime
    private long getEpochsFromLocalDateTime (LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        return localDateTime.atZone(zoneId).toEpochSecond(); // this is in seconds!!! not milliseconds!!!
    }

    public List<UsageStats> getDailyUsageStats() {
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = LocalDate.now().atTime(23, 59, 59);
        long start = getEpochsFromLocalDateTime(startTime);
        long end = getEpochsFromLocalDateTime(endTime);
        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start * 1000, end * 1000);
    }
}
