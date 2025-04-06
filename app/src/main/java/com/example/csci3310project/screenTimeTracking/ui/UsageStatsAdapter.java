package com.example.csci3310project.screenTimeTracking.ui;

import static androidx.recyclerview.widget.DiffUtil.calculateDiff;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3310project.R;
import com.example.csci3310project.screenTimeTracking.data.AppCategory;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

/**
 * Reference: RecyclerView | Everything You Need to Know https://www.youtube.com/watch?v=Mc0XT58A1Z4
 */

public class UsageStatsAdapter extends RecyclerView.Adapter<UsageStatsAdapter.UsageStatsViewHolder> {
    private List<UsageStatUIModel> usageStatsList;

    public UsageStatsAdapter(List<UsageStatUIModel> usageStatsList) {
        this.usageStatsList = usageStatsList;
    }

    // My Reference: 如何透過DiffUtil與ListAdapter來更新RecyclerView https://givemepass.medium.com/%E5%A6%82%E4%BD%95%E9%80%8F%E9%81%8Ediffutil%E8%88%87listadapter%E4%BE%86%E6%9B%B4%E6%96%B0recyclerview-1c0685472a3c
    // when the background service classify the app, it will update the item
    // previously, this will reset the whole list
    // now, it will only update the item that is changed !!!
    public void updateData(List<UsageStatUIModel> newData) {
        DiffUtil.DiffResult diffResult = calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return usageStatsList.size();
            }

            @Override
            public int getNewListSize() {
                return newData.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return usageStatsList.get(oldItemPosition).getAppName().equals(
                        newData.get(newItemPosition).getAppName());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                UsageStatUIModel oldItem = usageStatsList.get(oldItemPosition);
                UsageStatUIModel newItem = newData.get(newItemPosition);
                // we cannot compare the app icon, because it is a drawable
                // but i think the app icon will not change frequently
                return oldItem.getAppName().equals(newItem.getAppName()) &&
                        oldItem.getFormattedTime().equals(newItem.getFormattedTime()) &&
                        oldItem.getAppCategory().equals(newItem.getAppCategory());
            }
        });

        this.usageStatsList = new ArrayList<>(newData);
        diffResult.dispatchUpdatesTo(this);
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
        holder.packageNameTextView.setText(usageStatUIModel.getAppName());
        holder.timeTextView.setText(usageStatUIModel.getFormattedTime());
        holder.appIconImageView.setImageDrawable(usageStatUIModel.getAppIcon());

        String category = usageStatUIModel.getAppCategory();
        if (category.equals(AppCategory.PRODUCTIVE.getValue())) {
            holder.productivityTextView.setText(AppCategory.PRODUCTIVE.getValue());
            holder.productivityTextView.setBackgroundResource(R.drawable.section1_produtivity_tag_background);
        } else if (category.equals(AppCategory.NON_PRODUCTIVE.getValue())) {
            holder.productivityTextView.setText(AppCategory.NON_PRODUCTIVE.getValue());
            holder.productivityTextView.setBackgroundResource(R.drawable.section1_non_productive_tag_background);
        } else {
            holder.productivityTextView.setText("classifying");
            holder.productivityTextView.setBackgroundResource(R.drawable.section1_unclassified_tag_background);
        }
    }

    @Override
    public int getItemCount() {
        return usageStatsList.size();
    }

    public static class UsageStatsViewHolder extends RecyclerView.ViewHolder {
        public TextView packageNameTextView;
        public TextView timeTextView;

        public ImageView appIconImageView;

        public  TextView productivityTextView;

        public UsageStatsViewHolder(View view) {
            super(view);
            packageNameTextView = view.findViewById(R.id.package_name);
            timeTextView = view.findViewById(R.id.time);
            appIconImageView = view.findViewById(R.id.app_icon);
            productivityTextView = view.findViewById(R.id.productivity_tag);
        }
    }
}