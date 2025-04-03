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
    private List<UsageStatUIModel> usageStatsList;

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

    // use hours, minutes, seconds, omit the zero prefix.
    // eg. 0 hour 0 minute 5 seconds -> 5 seconds, 0 hour 1 minute 5 seconds -> 1 minute 5 seconds
    private String formatTime(long totalMsInForeground) {
        long seconds = (totalMsInForeground / 1000) % 60;
        long minutes = (totalMsInForeground / (1000 * 60)) % 60;
        long hours = (totalMsInForeground / (1000 * 60 * 60)) % 24;

        StringBuilder timeBuilder = new StringBuilder();
        if (hours > 0) {
            timeBuilder.append(hours).append(" hour ");
        }
        if (minutes > 0) {
            timeBuilder.append(minutes).append(" minute ");
        }
        if (seconds > 0) {
            timeBuilder.append(seconds).append(" seconds");
        }
        return timeBuilder.toString();
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull UsageStatsViewHolder holder, int position) {
        UsageStatUIModel usageStatUIModel = usageStatsList.get(position);
        holder.packageNameTextView.setText(usageStatUIModel.getPackageName());
        holder.timeTextView.setText(formatTime(usageStatUIModel.getTotalMsInForeground()));
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