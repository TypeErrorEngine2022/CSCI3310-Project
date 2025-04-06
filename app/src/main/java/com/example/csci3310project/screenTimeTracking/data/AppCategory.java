package com.example.csci3310project.screenTimeTracking.data;

public enum AppCategory {
    PRODUCTIVE("PRODUCTIVE"),
    NON_PRODUCTIVE("NON_PRODUCTIVE"),
    UNCLASSIFIED("UNCLASSIFIED");

    private final String value;

    AppCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String fromBoolean(boolean isProductive) {
        return isProductive ? PRODUCTIVE.toString() : NON_PRODUCTIVE.toString();
    }
}