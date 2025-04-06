package com.example.csci3310project.screenTimeTracking.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.domain.UsageStatsUseCase;

import java.util.List;

// My Reference:
// ViewModel Explained - Android Architecture Component | Tutorial, https://youtu.be/orH4K6qBzvE?si=dQ-4ZJR6e7wQzvCG
// LiveData Explained - Android Architecture Component | Tutorial, https://www.youtube.com/watch?v=suC0OM5gGAA

public class DashboardViewModel extends AndroidViewModel {
    private final LiveData<List<UsageStatUIModel>> usageStatsLiveData;

    public DashboardViewModel(Application application) {
        super(application);
        UsageRepository repository = new UsageRepository(application);
        UsageStatsUseCase usageStatsUseCase = new UsageStatsUseCase(repository, application);
        usageStatsLiveData = usageStatsUseCase.getUsageStatsLiveData();
    }

    public LiveData<List<UsageStatUIModel>> getUsageStats() {
        return usageStatsLiveData;
    }
}