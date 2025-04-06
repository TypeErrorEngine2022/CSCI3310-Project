package com.example.csci3310project.screenTimeTracking.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UsageDao {
    @Query("SELECT * FROM usage_entity")
    LiveData<List<UsageEntity>> getAll();

    @Query("SELECT * FROM usage_entity WHERE package_name LIKE :packageName")
    LiveData<UsageEntity> findByPackageName(String packageName);

    @Query("SELECT * FROM usage_entity WHERE package_name LIKE :packageName")
    UsageEntity findByPackageNameSync(String packageName);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insertUsageEntity(UsageEntity usageEntity);

    @Query("UPDATE usage_entity SET app_category = :category WHERE package_name = :packageName")
    void updateAppCategory(String packageName, String category);
}
