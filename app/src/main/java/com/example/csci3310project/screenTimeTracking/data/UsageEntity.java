package com.example.csci3310project.screenTimeTracking.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usage_entity")
public class UsageEntity {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "package_name")
    @NonNull
    public String packageName;

    @ColumnInfo(name = "total_ms_in_foreground")
    public long totalMsInForeground;

    @ColumnInfo(name = "app_icon_uri")
    public String appIconUri;

    @ColumnInfo(name = "app_category")
    @NonNull
    public String app_category;

    public UsageEntity() {
        this.packageName = "";
        this.totalMsInForeground = 0;
        this.app_category = "unclassified";
    }

    public UsageEntity(@NonNull String packageName, long totalMsInForeground) {
        this.packageName = packageName;
        this.totalMsInForeground = totalMsInForeground;
        this.app_category = "unclassified";
    }
}
