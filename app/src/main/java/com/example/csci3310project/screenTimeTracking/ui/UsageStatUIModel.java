package com.example.csci3310project.screenTimeTracking.ui;

import android.graphics.drawable.Drawable;

import com.example.csci3310project.screenTimeTracking.domain.AppUtils;

public class UsageStatUIModel {
    private final String packageName;

    private final String appName;

    private final Drawable appIcon;

    public long getTotalMsInForeground() {
        return totalMsInForeground;
    }

    private final long totalMsInForeground;

    private final String formattedTime;

    private final String app_category;

    public UsageStatUIModel(String packageName, String appName, long totalMsInForeground, Drawable appIcon, String app_category) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIcon = appIcon;
        this.totalMsInForeground = totalMsInForeground;
        this.formattedTime = AppUtils.formatTime(totalMsInForeground);
        this.app_category = app_category;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public String getAppCategory() {
        return app_category;
    }

    public String getPackageName() {
        return packageName;
    }
}