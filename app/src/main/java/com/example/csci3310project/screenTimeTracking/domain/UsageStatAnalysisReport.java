package com.example.csci3310project.screenTimeTracking.domain;

import java.util.List;

public class UsageStatAnalysisReport {
    /**
     * List of usage stats for productive apps. Sorted by time in descending order.
     */
    private final List<UsageStatAnalysisItem> productivityItems;

    /**
     * List of usage stats for non-productive apps. Sorted by time in descending order.
     */
    private final List<UsageStatAnalysisItem> nonProductivityItems;
    private final String totalProductivityTime;
    private final String totalNonProductivityTime;

    public String getTotalScreenTime() {
        return totalScreenTime;
    }

    private final String totalScreenTime;

    public UsageStatAnalysisReport(List<UsageStatAnalysisItem> productivityItems, List<UsageStatAnalysisItem> nonProductivityItems, long totalProductivityTime, long totalNonProductivityTime) {
        this.productivityItems = productivityItems;
        this.nonProductivityItems = nonProductivityItems;
        this.totalProductivityTime = AppUtils.formatTime(totalProductivityTime);
        this.totalNonProductivityTime = AppUtils.formatTime(totalNonProductivityTime);
        this.totalScreenTime = AppUtils.formatTime(totalProductivityTime + totalNonProductivityTime);
    }

    public List<UsageStatAnalysisItem> getProductivityItems() {
        return productivityItems;
    }

    public List<UsageStatAnalysisItem> getNonProductivityItems() {
        return nonProductivityItems;
    }

    public String getTotalProductivityTime() {
        return totalProductivityTime;
    }

    public String getTotalNonProductivityTime() {
        return totalNonProductivityTime;
    }
}
