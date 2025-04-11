package com.example.csci3310project.screenTimeTracking.domain;

public class UsageStatAnalysisItem {
    private final String appName;

    private final String formattedTime;

    /**
     * Usually blank for most apps.
     */
    private final String appDescription;

    public UsageStatAnalysisItem(String appName, String formattedTime, String appDescription) {
        this.appName = appName;
        this.formattedTime = formattedTime;
        this.appDescription = appDescription;
    }

    public String getAppName() {
        return appName;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public String getAppDescription() {
        return appDescription;
    }
}
