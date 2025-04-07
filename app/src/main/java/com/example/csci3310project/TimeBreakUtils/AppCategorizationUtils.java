package com.example.csci3310project.TimeBreakUtils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.example.csci3310project.screenTimeTracking.data.UsageEntity;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

public class AppCategorizationUtils {
    private static final String TAG = "AppCategorizationUtils";


    public static final String TYPE_ENTERTAINMENT = "Entertainment";
    public static final String TYPE_PRODUCTIVITY = "Productivity";
    public static final String TYPE_OTHER = "Other (treated as Entertainment)";

    // keep hardcode as spare
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

    private static UsageRepository repository;

    public static void initRepository(Context context) {
        if (repository == null) {
            repository = new UsageRepository(context.getApplicationContext());
            Log.d(TAG, "Repository initialized successfully");
        } else {
            Log.d(TAG, "Repository was already initialized");
        }
    }

    private static String queryAppType(String packageName) {
        Log.d(TAG, "queryAppType for package: " + packageName);

        if (packageName == null || packageName.isEmpty()) {
            return TYPE_OTHER;
        }

        // use database to identify
        if (repository != null) {
            try {
                final String[] appType = {null};
                final boolean[] completed = {false};

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        Log.d(TAG, "Querying database for: " + packageName);
                        UsageEntity entity = repository.getAppSync(packageName);

                        if (entity != null) {
                            Log.d(TAG, "Database result: " + packageName + " category=" + entity.app_category);

                            // 將 UsageEntity 的類型轉換為我們的類型
                            if ("PRODUCTIVE".equals(entity.app_category)) {
                                appType[0] = TYPE_PRODUCTIVITY;
                                Log.d(TAG, "Database classification: " + packageName + " as " + TYPE_PRODUCTIVITY);
                            } else if ("NON_PRODUCTIVE".equals(entity.app_category)) {
                                appType[0] = TYPE_ENTERTAINMENT;
                                Log.d(TAG, "Database classification: " + packageName + " as " + TYPE_ENTERTAINMENT);
                            } else {
                                Log.d(TAG, "Unclassified in database: " + entity.app_category);
                            }
                        } else {
                            Log.d(TAG, "No database entry found for " + packageName);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error querying app type: " + e.getMessage());
                    } finally {
                        completed[0] = true;
                    }
                });


                while (!completed[0]) {
                    Thread.sleep(10);
                }


                if (appType[0] != null) {
                    return appType[0];
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in queryAppType: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Repository is null! Cannot query database.");
        }

        // if database usage fail
        if (ENTERTAINMENT_APPS.contains(packageName)) {
            Log.d(TAG, "Hardcoded classification: " + packageName + " as " + TYPE_ENTERTAINMENT);
            return TYPE_ENTERTAINMENT;
        } else if (PRODUCTIVITY_APPS.contains(packageName)) {
            Log.d(TAG, "Hardcoded classification: " + packageName + " as " + TYPE_PRODUCTIVITY);
            return TYPE_PRODUCTIVITY;
        } else {
            Log.d(TAG, "Default classification: " + packageName + " as " + TYPE_OTHER);
            return TYPE_OTHER;
        }
    }


    public static boolean isEntertainmentApp(String packageName) {
        String appType = queryAppType(packageName);
        boolean result = TYPE_ENTERTAINMENT.equals(appType) || TYPE_OTHER.equals(appType);
        Log.d(TAG, "isEntertainmentApp: " + packageName + " = " + result);
        return result;
    }


    public static boolean isProductivityApp(String packageName) {
        String appType = queryAppType(packageName);
        boolean result = TYPE_PRODUCTIVITY.equals(appType);
        Log.d(TAG, "isProductivityApp: " + packageName + " = " + result);
        return result;
    }

    public static String getAppType(String packageName) {
        String appType = queryAppType(packageName);
        Log.d(TAG, "getAppType: " + packageName + " = " + appType);
        return appType;
    }

    public static String getAppName(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            Log.d(TAG, "App name for " + packageName + ": " + appName);
            return appName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find app name for " + packageName + ": " + e.getMessage());
            return packageName; // Return package name if app name is not found
        }
    }
}