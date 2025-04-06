package com.example.csci3310project.TimeBreakUtils;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AppCategorizationUtils {
    private static final Set<String> ENTERTAINMENT_APPS = new HashSet<>(Arrays.asList(
            "com.facebook.katana", // Facebook
            "com.instagram.android", // Instagram
            "com.google.android.youtube", // YouTube
            "com.twitter.android", // Twitter
            "com.snapchat.android", // Snapchat
            "com.spotify.music", // Spotify
            "com.netflix.mediaclient", // Netflix
            "com.tiktok.music" // TikTok
    ));

    private static final Set<String> PRODUCTIVITY_APPS = new HashSet<>(Arrays.asList(
            "com.microsoft.office.word", // MS Word
            "com.microsoft.office.excel", // MS Excel
            "com.microsoft.office.powerpoint", // MS PowerPoint
            "com.google.android.gm", // Gmail
            "com.google.android.apps.docs", // Google Docs
            "com.google.android.apps.spreadsheet", // Google Sheets
            "com.slack", // Slack
            "com.microsoft.teams" // Microsoft Teams
    ));

    public static boolean isEntertainmentApp(String packageName) {
        return ENTERTAINMENT_APPS.contains(packageName);
    }

    public static boolean isProductivityApp(String packageName) {
        return PRODUCTIVITY_APPS.contains(packageName);
    }

    public static String getAppType(String packageName) {
        if (isEntertainmentApp(packageName)) {
            return "Entertainment";
        } else if (isProductivityApp(packageName)) {
            return "Productivity";
        } else {
            return "Other (treated as Entertainment)";
        }
    }

    public static String getAppName(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName; // Return package name if app name is not found
        }
    }

}
