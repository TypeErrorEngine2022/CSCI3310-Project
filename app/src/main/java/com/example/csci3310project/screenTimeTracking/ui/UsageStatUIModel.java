package com.example.csci3310project.screenTimeTracking.ui;

import android.graphics.drawable.Drawable;

public class UsageStatUIModel {
    private String packageName;
    private long totalMsInForeground;

    Drawable appIcon;

    public UsageStatUIModel(String packageName, long totalMsInForeground, Drawable appIcon) {
        this.packageName = packageName;
        this.totalMsInForeground = totalMsInForeground;
        this.appIcon = appIcon;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getTotalMsInForeground() {
        return totalMsInForeground;
    }

    public void setTotalMsInForeground(long totalMsInForeground) {
        this.totalMsInForeground = totalMsInForeground;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }
}