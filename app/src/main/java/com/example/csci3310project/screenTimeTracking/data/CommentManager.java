package com.example.csci3310project.screenTimeTracking.data;

import android.content.Context;
import android.content.SharedPreferences;

public class CommentManager {
    private static final String PREFS_NAME = "comment_cache";
    private static final String KEY_COMMENT = "comment";
    private static final String KEY_TIMESTAMP = "timestamp";

    private static final int COMMENT_INTERVAL = 15 * 60 * 1000; // 15 minutes

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
        if (System.currentTimeMillis() - timestamp < COMMENT_INTERVAL) {
            return prefs.getString(KEY_COMMENT, null);
        }
        return null;
    }

    public static void setCommentRequestTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public static boolean isCommentRequestAllowed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long timestamp = prefs.getLong(KEY_TIMESTAMP, 0);
        // if the last request was more than COMMENT_INTERVAL minutes ago, allow new request
        // this is to prevent spamming the LLM with requests, again, please test the app by switching back and forth between fragments ~
        return System.currentTimeMillis() - timestamp >= COMMENT_INTERVAL;
    }
}