package com.example.csci3310project.screenTimeTracking.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3310project.R;
import com.example.csci3310project.screenTimeTracking.data.UsageEntity;

import java.util.List;

public class UsageStatsAdapter extends RecyclerView.Adapter<UsageStatsAdapter.ViewHolder> {
    private List<UsageEntity> usageStatsList;

    public UsageStatsAdapter(List<UsageEntity> usageStatsList) {
        this.usageStatsList = usageStatsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.section1_usage_stats_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsageEntity usageEntity = usageStatsList.get(position);
        holder.packageNameTextView.setText("Package: " + usageEntity.packageName);
        holder.timeTextView.setText("Time: " + usageEntity.totalMsInForeground / 1000 + " seconds");
    }

    @Override
    public int getItemCount() {
        return usageStatsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView packageNameTextView;
        public TextView timeTextView;

        public ViewHolder(View view) {
            super(view);
            packageNameTextView = view.findViewById(R.id.package_name);
            timeTextView = view.findViewById(R.id.time);
        }
    }
}