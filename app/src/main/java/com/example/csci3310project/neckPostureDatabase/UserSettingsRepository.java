package com.example.csci3310project.neckPostureDatabase;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

public class UserSettingsRepository {
    private UserSettingsDao userSettingsDao;
    private LiveData<UserSettings> settings;

    public UserSettingsRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userSettingsDao = db.userSettingsDao();
        settings = userSettingsDao.getSettings();
    }

    public LiveData<UserSettings> getSettings() {
        return settings;
    }

    public void insert(UserSettings settings) {
        new Thread(() -> {
            userSettingsDao.insert(settings);
            UserSettings afterInsert = userSettingsDao.getSettingsSync();
            Log.d("My_debug", "UserSettingsRepository.insert called");
            Log.d("My_debug", "After insert: " + afterInsert.toString());
        }).start();
    }

    public void update(UserSettings settings) {
        new Thread(() -> {
            userSettingsDao.update(settings);
            UserSettings afterUpdate = userSettingsDao.getSettingsSync();
            Log.d("My_debug", "UserSettingsRepository.update called");
            Log.d("My_debug", "After update: " + afterUpdate.toString());
        }).start();
    }
}