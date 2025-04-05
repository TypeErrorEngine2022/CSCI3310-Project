package com.example.csci3310project.screenTimeTracking.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

@Dao
public interface UsageDao {
    @Query("SELECT * FROM usage_entity")
    List<UsageEntity> getAll();

    @Query("SELECT * FROM usage_entity WHERE package_name LIKE :packageName")
    UsageEntity findByPackageName(String packageName);

    @Upsert()
    void upsertUsageEntity(UsageEntity usageEntity);
}
