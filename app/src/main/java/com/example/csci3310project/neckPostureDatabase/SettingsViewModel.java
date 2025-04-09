package com.example.csci3310project.neckPostureDatabase;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.app.Application;
import android.util.Log;

import com.example.csci3310project.neckPostureDatabase.*;

public class SettingsViewModel extends AndroidViewModel {
    private UserSettingsRepository repository;
    private LiveData<UserSettings> settings;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        repository = new UserSettingsRepository(application);
        settings = repository.getSettings();
    }

    public LiveData<UserSettings> getSettings() {
        Log.d("My_debug","SettingsViewModel.getSettings() called");
        return settings;
    }

    public void updateSettings(UserSettings settings) {
        Log.d("My_debug","SettingsViewModel.updateSettings() called");
        repository.update(settings);
    }

    public void ensureDefaults() {
        Log.d("My_debug","SettingsViewModel.ensureDefaults() called");
        UserSettings defaultSettings = new UserSettings();
        defaultSettings.id = 1;
        defaultSettings.postureAnalysisEnabled = false;
        defaultSettings.checkInterval = "5 seconds";
        defaultSettings.notificationsEnabled = true;
        defaultSettings.toleranceLevel = "Medium";
        repository.insert(defaultSettings);
    }
}