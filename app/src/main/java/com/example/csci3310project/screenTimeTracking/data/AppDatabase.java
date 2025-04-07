package com.example.csci3310project.screenTimeTracking.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// My Reference: Android Room 資料庫使用方法 https://blog.tarswork.com/post/android-storage-using-room

@Database(entities = {UsageEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public abstract UsageDao usageDao();

    // again, let's use singleton ~
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class, "screen_time_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}