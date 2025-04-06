package com.example.csci3310project.screenTimeTracking.ui;

import android.graphics.drawable.Drawable;

public class UsageStatUIModel {
    private final String appName;

    private final Drawable appIcon;

    private final String formattedTime;

    private final boolean isProductive;

    public UsageStatUIModel(String appName, long totalMsInForeground, Drawable appIcon, boolean isProductive) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.formattedTime = formatTime(totalMsInForeground);
        this.isProductive = isProductive;
    }

    public String getAppName() {
        return appName;
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

    public boolean isProductive() {
        return isProductive;
    }
}