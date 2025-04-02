package com.example.csci3310project.screenTimeTracking.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3310project.R;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.data.UsageStatsDataSource;
import com.example.csci3310project.screenTimeTracking.domain.UsageStatsUseCase;

import java.util.List;

public class DashboardFragment extends Fragment {
    UsageStatsUseCase usageStatsUseCase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1_dashboard, container, false);

        Context context = getContext();
        assert context != null;
        usageStatsUseCase = new UsageStatsUseCase(new UsageRepository(new UsageStatsDataSource(context)), context);

        RecyclerView usageStatsRecyclerView = view.findViewById(R.id.usage_stats_recycler_view);
        usageStatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<UsageStatUIModel> usageStatsData = usageStatsUseCase.getDailyUsageStats();
        UsageStatsAdapter adapter = new UsageStatsAdapter(usageStatsData);
        usageStatsRecyclerView.setAdapter(adapter);

        return view;
    }
}