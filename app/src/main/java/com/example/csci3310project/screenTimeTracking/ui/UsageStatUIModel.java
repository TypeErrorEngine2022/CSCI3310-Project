package com.example.csci3310project.screenTimeTracking.ui;

import android.graphics.drawable.Drawable;

public class UsageStatUIModel {
    private final String packageName;
    private final long totalMsInForeground;

    private final Drawable appIcon;

    private final String formattedTime;

    public UsageStatUIModel(String packageName, long totalMsInForeground, Drawable appIcon) {
        this.packageName = packageName;
        this.totalMsInForeground = totalMsInForeground;
        this.appIcon = appIcon;
        this.formattedTime = formatTime(totalMsInForeground);
    }

    public String getPackageName() {
        return packageName;
    }

    public long getTotalMsInForeground() {
        return totalMsInForeground;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    // use hours, minutes, seconds, omit the zero prefix.
    // eg. 0 hour 0 minute 5 seconds -> 5 seconds, 0 hour 1 minute 5 seconds -> 1 minute 5 seconds
    private String formatTime(long totalMsInForeground) {
        long seconds = (totalMsInForeground / 1000) % 60;
        long minutes = (totalMsInForeground / (1000 * 60)) % 60;
        long hours = (totalMsInForeground / (1000 * 60 * 60)) % 24;

        StringBuilder timeBuilder = new StringBuilder();
        if (hours > 0) {
            timeBuilder.append(hours).append(" hour ");
        }
        if (minutes > 0) {
            timeBuilder.append(minutes).append(" minute ");
        }
        if (seconds > 0) {
            timeBuilder.append(seconds).append(" seconds");
        }
        return timeBuilder.toString();
    }

    public String getFormattedTime() {
        return formattedTime;
    }
}