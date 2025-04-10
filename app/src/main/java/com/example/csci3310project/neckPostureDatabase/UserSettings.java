package com.example.csci3310project.neckPostureDatabase;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_settings")
public class UserSettings {
    @PrimaryKey
    public int id = 1;
    public boolean postureAnalysisEnabled;
    public String checkInterval;
    public boolean notificationsEnabled;
    public String toleranceLevel;

    @Override
    public String toString() {
        return "UserSettings{" +
                "id=" + id +
                ", postureAnalysisEnabled=" + postureAnalysisEnabled +
                ", checkInterval='" + (checkInterval != null ? checkInterval : "not set") + '\'' +
                ", notificationsEnabled=" + notificationsEnabled +
                ", toleranceLevel='" + (toleranceLevel != null ? toleranceLevel : "not set") + '\'' +
                '}';
    }
}