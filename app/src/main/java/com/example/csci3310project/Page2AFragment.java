package com.example.csci3310project;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Page2AFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "Page2AFragment";
    private static final String CHANNEL_ID = "break_reminder_channel";

    private TextView textView;
    private Button button;
    private UsageStatsManager usageStatsManager;
    private boolean isMonitoring = false;
    private Thread monitoringThread;
    private Section2Fragment parentFragment;

    private String currentForegroundApp = "";
    private long appUsageStartTime = 0;
    private boolean isInBreak = false;
    private CountDownTimer breakTimer;

    // App categorization
    private Set<String> entertainmentApps = new HashSet<>(Arrays.asList(
            "com.facebook.katana", // Facebook
            "com.instagram.android", // Instagram
            "com.google.android.youtube", // YouTube
            "com.twitter.android", // Twitter
            "com.snapchat.android", // Snapchat
            "com.spotify.music", // Spotify
            "com.netflix.mediaclient", // Netflix
            "com.tiktok.music" // TikTok
    ));

    private Set<String> productivityApps = new HashSet<>(Arrays.asList(
            "com.microsoft.office.word", // MS Word
            "com.microsoft.office.excel", // MS Excel
            "com.microsoft.office.powerpoint", // MS PowerPoint
            "com.google.android.gm", // Gmail
            "com.google.android.apps.docs", // Google Docs
            "com.google.android.apps.spreadsheet", // Google Sheets
            "com.slack", // Slack
            "com.microsoft.teams" // Microsoft Teams
    ));

    // Constructor with parent fragment
    public Page2AFragment() {
        // Required empty constructor
    }

    public void setParentFragment(Section2Fragment parentFragment) {
        this.parentFragment = parentFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page2a, container, false);
        textView = view.findViewById(R.id.textView);
        button = view.findViewById(R.id.button);
        button.setOnClickListener(this);

        usageStatsManager = (UsageStatsManager) getActivity().getSystemService(Context.USAGE_STATS_SERVICE);

        // Create notification channel
        createNotificationChannel();

        // version check(only for Android 13 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (getActivity().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission Allowed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No Permission, fail to offer alarm", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (button.getText().toString().equals("Start")) {
            startMonitoring();
            button.setText("End");

            // Notify parent to disable settings page
            if (parentFragment != null) {
                parentFragment.setMonitoringActive(true);
            }
        } else {
            stopMonitoring();
            button.setText("Start");

            // Notify parent that monitoring has stopped
            if (parentFragment != null) {
                parentFragment.setMonitoringActive(false);
            }
        }
    }

    private void startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true;
            textView.setText("Monitoring started...");

            monitoringThread = new Thread(() -> {
                while (isMonitoring) {
                    try {
                        // Get current foreground app
                        String foregroundApp = getCurrentForegroundApp();

                        if (foregroundApp != null && !foregroundApp.equals(getActivity().getPackageName())) {
                            // If app changed, reset timer
                            if (!foregroundApp.equals(currentForegroundApp)) {
                                handleAppSwitch(foregroundApp);
                            }

                            // Check if we need to show break reminder
                            checkForBreakTime(foregroundApp);

                            // Update the UI
                            updateUI(foregroundApp);
                        }

                        Thread.sleep(1000); // Check every second
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Monitoring thread interrupted", e);
                    }
                }
            });
            monitoringThread.start();
        }
    }

    private void stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            if (monitoringThread != null) {
                monitoringThread.interrupt();
            }
            if (breakTimer != null) {
                breakTimer.cancel();
            }
            isInBreak = false;
            textView.setText("Monitoring stopped");
        }
    }

    private String getCurrentForegroundApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - 10000; // Look back 10 seconds

            UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();
            UsageEvents.Event lastEvent = null;

            // Find last MOVE_TO_FOREGROUND event
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastEvent = event;
                }
            }

            if (lastEvent != null) {
                return lastEvent.getPackageName();
            }
        }
        return null;
    }

    private void handleAppSwitch(String newApp) {
        // Reset timers when switching apps
        currentForegroundApp = newApp;
        appUsageStartTime = System.currentTimeMillis();
        isInBreak = false;

        if (breakTimer != null) {
            breakTimer.cancel();
        }

        // Log the app switch
        getActivity().runOnUiThread(() -> {
            textView.setText("Now using: " + getAppName(newApp) + "\nType: " + getAppType(newApp));
        });
    }

    private void checkForBreakTime(String packageName) {
        if (isInBreak || appUsageStartTime == 0) {
            return; // Already in break or not tracking yet
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - appUsageStartTime;

        // Get relevant work duration based on app type
        long workDuration = 0;
        if (parentFragment != null) {
            if (isEntertainmentApp(packageName)) {
                workDuration = parentFragment.getEntertainmentWorkDuration();
            } else if (isProductivityApp(packageName)) {
                workDuration = parentFragment.getProductivityWorkDuration();
            } else {
                // Default to entertainment settings for unknown apps
                workDuration = parentFragment.getEntertainmentWorkDuration();
            }
        }

        if (elapsedTime >= workDuration) {
            // Time to take a break
            startBreakTime(packageName);
        }
    }

    private void startBreakTime(String packageName) {
        isInBreak = true;

        // Get break duration based on app type
        final long breakDuration;
        if (parentFragment != null) {
            if (isEntertainmentApp(packageName)) {
                breakDuration = parentFragment.getEntertainmentBreakDuration();
            } else if (isProductivityApp(packageName)) {
                breakDuration = parentFragment.getProductivityBreakDuration();
            } else {
                // Default to entertainment settings for unknown apps
                breakDuration = parentFragment.getEntertainmentBreakDuration();
            }
        } else {
            // Fallback to default values if parent is not available
            breakDuration = 5 * 60 * 1000; // 5 minutes
        }

        // Show notification
        showBreakNotification(getAppName(packageName), breakDuration);

        // Show alert dialog
        getActivity().runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Time for a break!")
                    .setMessage("You've been using " + getAppName(packageName) + " for too long. Take a break for " +
                            (breakDuration / 60000) + " minutes.")
                    .setPositiveButton("OK", null)
                    .setCancelable(false)
                    .show();
        });

        // Start break countdown timer
        breakTimer = new CountDownTimer(breakDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                final String timeLeft = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))
                );

                getActivity().runOnUiThread(() -> {
                    textView.setText("BREAK TIME!\nTime left: " + timeLeft);
                });
            }

            @Override
            public void onFinish() {
                isInBreak = false;
                appUsageStartTime = System.currentTimeMillis(); // Reset usage timer

                // Notify user that break is over
                getActivity().runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Break finished")
                            .setMessage("You can resume using " + getAppName(packageName) + " now.")
                            .setPositiveButton("OK", null)
                            .setCancelable(false)
                            .show();

                    textView.setText("Now using: " + getAppName(packageName) + "\nType: " + getAppType(packageName));
                });
            }
        };

        breakTimer.start();
    }

    private void updateUI(String packageName) {
        if (isInBreak || packageName == null) {
            return; // UI is handled by break timer
        }

        final long elapsedTime = System.currentTimeMillis() - appUsageStartTime;
        final long workDuration = getWorkDurationForApp(packageName);
        final long timeLeft = workDuration - elapsedTime;

        if (timeLeft > 0) {
            final String timeLeftStr = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(timeLeft),
                    TimeUnit.MILLISECONDS.toSeconds(timeLeft) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeft))
            );

            getActivity().runOnUiThread(() -> {
                textView.setText("Now using: " + getAppName(packageName) +
                        "\nType: " + getAppType(packageName) +
                        "\nTime until break: " + timeLeftStr);
            });
        }
    }

    private long getWorkDurationForApp(String packageName) {
        if (parentFragment != null) {
            if (isEntertainmentApp(packageName)) {
                return parentFragment.getEntertainmentWorkDuration();
            } else if (isProductivityApp(packageName)) {
                return parentFragment.getProductivityWorkDuration();
            } else {
                // Default to entertainment settings for unknown apps
                return parentFragment.getEntertainmentWorkDuration();
            }
        }
        // Fallback values
        return 30 * 60 * 1000; // 30 minutes
    }

    private boolean isEntertainmentApp(String packageName) {
        return entertainmentApps.contains(packageName);
    }

    private boolean isProductivityApp(String packageName) {
        return productivityApps.contains(packageName);
    }

    private String getAppType(String packageName) {
        if (isEntertainmentApp(packageName)) {
            return "Entertainment";
        } else if (isProductivityApp(packageName)) {
            return "Productivity";
        } else {
            return "Other (treated as Entertainment)";
        }
    }

    private String getAppName(String packageName) {
        PackageManager packageManager = getActivity().getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private void showBreakNotification(String appName, long breakDuration) {
        // permission check 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                getActivity().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return; // if no permission, do not show notification
        }

        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Take a break!")
                .setContentText("You have used " + appName + " too long, take a braek for " +
                        (breakDuration / 60000) + "min please!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 1000})
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Break Reminders";
            String description = "Notifications for app break reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}