package com.example.csci3310project.screenTimeTracking.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.ProgressBar;

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

    private RecyclerView usageStatsRecyclerView;
    private ProgressBar loadingProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1_dashboard, container, false);

        usageStatsRecyclerView = view.findViewById(R.id.usage_stats_recycler_view);
        usageStatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadingProgressBar = view.findViewById(R.id.dashboard_progress_bar);

        showLoading(true);

        adapter = new UsageStatsAdapter(new ArrayList<>());
        usageStatsRecyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DashboardViewModel viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        adapter = new UsageStatsAdapter(new ArrayList<>());
        usageStatsRecyclerView.setAdapter(adapter);

        // the reason to observer usage stat, instead of getting static list
        // because the background service will classify the app, which will update the item
        viewModel.getUsageStats().observe(getViewLifecycleOwner(), usageStats -> {
            showLoading(false);
            adapter.updateData(usageStats);
        });
    }

    private void showLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        usageStatsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}