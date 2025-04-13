package com.example.csci3310project.TimeBreakUtils;

import android.graphics.drawable.Drawable;

public class AppUsageData {
    private String packageName;
    private Drawable appIcon;
    private String appName;
    private String appType;
    private long usageDuration;
    private long lastUsed;

    public AppUsageData(String packageName, Drawable appIcon, String appName, String appType, long usageDuration, long lastUsed) {
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.appName = appName;
        this.appType = appType;
        this.usageDuration = usageDuration;
        this.lastUsed = lastUsed;
    }

    public String getPackageName() { return packageName; }
    public Drawable getAppIcon() { return appIcon; }
    public String getAppName() { return appName; }
    public String getAppType() { return appType; }
    public long getUsageDuration() { return usageDuration; }
    public long getLastUsed() { return lastUsed; }

    public String getFormattedDuration() {
        long seconds = usageDuration / 1000;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
