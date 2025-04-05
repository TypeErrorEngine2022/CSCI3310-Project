package com.example.csci3310project.screenTimeTracking.data;

import android.content.Context;
import android.content.SharedPreferences;

// Reference: Save simple data with SharedPreferences https://developer.android.com/training/data-storage/shared-preferences

public class CommentManager {
    private static final String PREFS_NAME = "comment_cache";
    private static final String KEY_COMMENT = "comment";
    private static final String KEY_TIMESTAMP = "timestamp";

    public static void saveComment(Context context, String comment) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_COMMENT, comment);
        editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public static String getCommentIfValid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long timestamp = prefs.getLong(KEY_TIMESTAMP, 0);
        // if the comment is less than 15 minutes old, return it
        if (System.currentTimeMillis() - timestamp < 15 * 60 * 1000) {
            return prefs.getString(KEY_COMMENT, null);
        }
        return null;
    }
}
