package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class AppUtils {
    private static  final String TAG = "AppUtils";
    // My Reference: https://stackoverflow.com/questions/17985500/how-can-i-get-the-applications-icon-from-the-package-name
    public static Drawable getAppIcon(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting app icon: " + packageName, e);
        }
        return null;
    }

    // My Reference: https://blog.csdn.net/qq_37858386/article/details/124501617
    public static boolean isSystemApp(Context context, String packageName) {
        try {
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
            assert packageInfo.applicationInfo != null;
            return (packageInfo.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
            return false;
        }
    }

    // My Reference: Android应用层PackageManager的使用 https://www.cnblogs.com/dony-c/p/9478115.html
    public static String getAppName(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
            return packageName; // Fallback to package name if app name is not found
        }
    }

    // use hours, minutes, seconds, omit the zero prefix.
    // eg. 0 hour 0 minute 5 seconds -> 5 seconds, 0 hour 1 minute 5 seconds -> 1 minute 5 seconds
    public static String formatTime(long totalMsInForeground) {
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
        return timeBuilder.toString().trim();
    }
}
