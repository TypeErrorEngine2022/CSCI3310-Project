package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class AppUtils {
    private static  final String TAG = "AppUtils";
    // reference: https://stackoverflow.com/questions/17985500/how-can-i-get-the-applications-icon-from-the-package-name
    public static Drawable getAppIcon(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
        }
        return null;
    }

    // reference: https://blog.csdn.net/qq_37858386/article/details/124501617
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

    // Reference: Android应用层PackageManager的使用 https://www.cnblogs.com/dony-c/p/9478115.html
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
}
