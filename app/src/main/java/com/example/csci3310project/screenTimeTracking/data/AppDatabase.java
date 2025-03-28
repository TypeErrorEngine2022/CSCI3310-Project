package com.example.csci3310project.screenTimeTracking.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {UsageEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UsageDao usageDao();

}
