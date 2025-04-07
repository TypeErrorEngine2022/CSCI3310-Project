package com.example.csci3310project.screenTimeTracking.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.domain.UsageStatsUseCase;

import java.util.List;

// My Reference:
// ViewModel Explained - Android Architecture Component | Tutorial, https://youtu.be/orH4K6qBzvE?si=dQ-4ZJR6e7wQzvCG
// LiveData Explained - Android Architecture Component | Tutorial, https://www.youtube.com/watch?v=suC0OM5gGAA
// Day 15 LiveData 介紹與使用 https://ithelp.ithome.com.tw/articles/10222799?sc=rss.iron

public class DashboardViewModel extends AndroidViewModel {
    private final MediatorLiveData<List<UsageStatUIModel>> usageStatsLiveData = new MediatorLiveData<>();
    private final UsageStatsUseCase usageStatsUseCase;
    private LiveData<List<UsageStatUIModel>> currentSource;

    public DashboardViewModel(Application application) {
        super(application);
        UsageRepository repository = new UsageRepository(application);
        usageStatsUseCase = new UsageStatsUseCase(repository, application);
        currentSource = usageStatsUseCase.getUsageStatsLiveData();
        usageStatsLiveData.addSource(currentSource, usageStatsLiveData::setValue);
    }

    public LiveData<List<UsageStatUIModel>> getUsageStats() {
        return usageStatsLiveData;
    }

    public void refreshData() {
        // Remove the previous source
        if (currentSource != null) {
            usageStatsLiveData.removeSource(currentSource);
        }

        // Add the new source
        currentSource = usageStatsUseCase.getUsageStatsLiveData();
        usageStatsLiveData.addSource(currentSource, usageStatsLiveData::setValue);
    }
}