package com.example.csci3310project.screenTimeTracking.ui;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3310project.R;

import java.util.ArrayList;

public class DashboardFragment extends Fragment {
    private UsageStatsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1_dashboard, container, false);

        RecyclerView usageStatsRecyclerView = view.findViewById(R.id.usage_stats_recycler_view);
        usageStatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new UsageStatsAdapter(new ArrayList<>());
        usageStatsRecyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DashboardViewModel viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // the reason to observer usage stat, instead of getting static list
        // because the background service will classify the app, which will update the item
        viewModel.getUsageStats().observe(getViewLifecycleOwner(), usageStats -> {
            adapter = new UsageStatsAdapter(usageStats);
            RecyclerView usageStatsRecyclerView = view.findViewById(R.id.usage_stats_recycler_view);
            usageStatsRecyclerView.setAdapter(adapter);
        });
    }
}