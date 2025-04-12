package com.example.csci3310project.TimeBreakUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3310project.R;

import java.util.ArrayList;
import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.ViewHolder> {

    private List<AppUsageData> appUsageList = new ArrayList<>();

    public AppUsageAdapter(List<AppUsageData> appUsageList) {
        this.appUsageList = appUsageList;
    }

    public void updateData(List<AppUsageData> newData) {
        this.appUsageList.clear();
        this.appUsageList.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppUsageData data = appUsageList.get(position);
        holder.appIcon.setImageDrawable(data.getAppIcon());
        holder.appName.setText(data.getAppName());
        holder.appType.setText(data.getAppType());
        holder.usageDuration.setText(data.getFormattedDuration());
    }

    @Override
    public int getItemCount() {
        return appUsageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView appType;
        TextView usageDuration;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appType = itemView.findViewById(R.id.appType);
            usageDuration = itemView.findViewById(R.id.usageDuration);
        }
    }
}