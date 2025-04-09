package com.example.csci3310project.neckPostureDatabase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    LiveData<UserSettings> getSettings();

    @Query("SELECT * FROM user_settings WHERE id = 1")
        UserSettings getSettingsSync();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(UserSettings settings);

    @Update
    void update(UserSettings settings);
}