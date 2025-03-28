package com.example.csci3310project.screenTimeTracking.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.example.csci3310project.R;
import com.example.csci3310project.screenTimeTracking.data.UsageEntity;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.data.UsageStatsDataSource;
import com.example.csci3310project.screenTimeTracking.domain.UsageStatsUseCase;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    UsageStatsUseCase usageStatsUseCase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1_dashboard, container, false);

        usageStatsUseCase = new UsageStatsUseCase(new UsageRepository(new UsageStatsDataSource(getContext())));

        ListView usageStatsList = view.findViewById(R.id.usage_stats_list);
        List<String> usageStatsData = getUsageStatsData();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, usageStatsData);
        usageStatsList.setAdapter(adapter);

        return view;
    }

    private List<String> getUsageStatsData() {
        List<UsageEntity> usageStatsList = usageStatsUseCase.getDailyUsageStats();
        List<String> usageStatsData = new ArrayList<>();

        for (UsageEntity entity : usageStatsList) {
            usageStatsData.add("Package: " + entity.packageName + ", Time: " + entity.totalMsInForeground / 1000 + " seconds");
        }

        return usageStatsData;
    }
}