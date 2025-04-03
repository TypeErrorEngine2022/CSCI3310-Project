package com.example.csci3310project.screenTimeTracking.ui;

import android.graphics.drawable.Drawable;

public class UsageStatUIModel {
    private final String packageName;
    private final long totalMsInForeground;

    private final Drawable appIcon;

    public UsageStatUIModel(String packageName, long totalMsInForeground, Drawable appIcon) {
        this.packageName = packageName;
        this.totalMsInForeground = totalMsInForeground;
        this.appIcon = appIcon;
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
}