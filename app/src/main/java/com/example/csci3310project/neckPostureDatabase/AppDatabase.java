package com.example.csci3310project.neckPostureDatabase;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UserSettings.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserSettingsDao userSettingsDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        Log.d("My_debug","AppDatabase.getDatabase() called");
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class,
                                    "app_database")
                            .allowMainThreadQueries() // Not recommended for production
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}