package com.example.csci3310project.screenTimeTracking.domain;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.csci3310project.screenTimeTracking.data.UsageRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PromptGenerator {
    public interface PromptCallback {
        void onPromptReady(String prompt);
    }

    private static void appendTopApps(StringBuilder prompt, List<UsageStatAnalysisItem> apps, String title) {
        prompt.append(title).append("[");
        for (int i = 0; i < Math.min(5, apps.size()); i++) {
            UsageStatAnalysisItem app = apps.get(i);
            prompt.append("`").append(app.getAppName()).append(" ");
            if (app.getAppDescription() != null && !app.getAppDescription().isEmpty()) {
                prompt.append("(").append(app.getAppDescription()).append(")");
            }
            prompt.append(app.getFormattedTime()).append("`,");
        }
        if (apps.size() > 1) {
            prompt.setLength(prompt.length() - 1); // remove the last comma of the last item
        }
        prompt.append("]\n");
    }

    public static void getPromptAsync(Context context, PromptCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            StringBuilder prompt = new StringBuilder();
            UsageStatsUseCase usageStatsUseCase = new UsageStatsUseCase(new UsageRepository(context), context);
            UsageStatAnalysisReport usageStatAnalysisReport = usageStatsUseCase.getUsageStatAnalysisReportSync();
            List<UsageStatAnalysisItem> productivityApps = usageStatAnalysisReport.getProductivityItems();
            List<UsageStatAnalysisItem> nonProductivityApps = usageStatAnalysisReport.getNonProductivityItems();

            prompt.append("User's total screen time today: ").append(usageStatAnalysisReport.getTotalScreenTime()).append("\n");

            appendTopApps(prompt, productivityApps, "User's Top 5 used Productive Apps:");
            prompt.append("Total Productive Time: ").append(usageStatAnalysisReport.getTotalProductivityTime()).append("\n");

            appendTopApps(prompt, nonProductivityApps, "User's Top 5 used Non-Productive Apps:");
            prompt.append("Total Non-Productive Time: ").append(usageStatAnalysisReport.getTotalNonProductivityTime()).append("\n");

            prompt.append("You are an AI assistant to encourage user to be more PRODUCTIVE. Give advice on the user's productivity. DO NOT ASK FOLLOW-UP QUESTIONS. DO NOT HALLUCINATE.\n");
            prompt.append("Encourage the usage of productive apps.\n");
            prompt.append("Discourage the usage of non-productive apps.\n");
            prompt.append("Give specific, actionable advice like setting screen time limits, using productivity techniques (Pomodoro, time blocking), or suggesting productive alternatives to non-productive apps.\n");
            prompt.append("Sample advice: Your screen time today is quite interesting! You spent 5 hours on {app}, which shows you are very dedicated to your work—well done! However, your usage of {app} is also a bit high, which might affect your focus. Try to shift some time to more productive activities, and you'll be even more efficient! Keep it up; you can do it!\n");
            prompt.append("Your comment:\n");

            new Handler(Looper.getMainLooper()).post(() ->
                    callback.onPromptReady(prompt.toString())
            );
        });

        // avoid memory leak
        executor.shutdown();
    }
}
