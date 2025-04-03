package com.example.csci3310project.screenTimeTracking.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3310project.R;

import java.util.List;

/**
 * Reference: RecyclerView | Everything You Need to Know https://www.youtube.com/watch?v=Mc0XT58A1Z4
 */

public class UsageStatsAdapter extends RecyclerView.Adapter<UsageStatsAdapter.UsageStatsViewHolder> {
    private final List<UsageStatUIModel> usageStatsList;

    public UsageStatsAdapter(List<UsageStatUIModel> usageStatsList) {
        this.usageStatsList = usageStatsList;
    }

    @NonNull
    @Override
    public UsageStatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.section1_usage_stats_item, parent, false);
        return new UsageStatsViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull UsageStatsViewHolder holder, int position) {
        UsageStatUIModel usageStatUIModel = usageStatsList.get(position);
        holder.packageNameTextView.setText(usageStatUIModel.getPackageName());
        holder.timeTextView.setText(usageStatUIModel.getFormattedTime());
        holder.appIconImageView.setImageDrawable(usageStatUIModel.getAppIcon());
    }

    @Override
    public int getItemCount() {
        return usageStatsList.size();
    }

    public static class UsageStatsViewHolder extends RecyclerView.ViewHolder {
        public TextView packageNameTextView;
        public TextView timeTextView;

        public ImageView appIconImageView;

        public UsageStatsViewHolder(View view) {
            super(view);
            packageNameTextView = view.findViewById(R.id.package_name);
            timeTextView = view.findViewById(R.id.time);
            appIconImageView = view.findViewById(R.id.app_icon);
        }
    }
}