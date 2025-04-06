package com.example.csci3310project;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
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
import com.example.csci3310project.TimeBreakUtils.MonitoringService;
import com.example.csci3310project.TimeBreakUtils.AppCategorizationUtils;

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

    private boolean isEntertainmentApp(String packageName){
        return AppCategorizationUtils.isEntertainmentApp(packageName);
    }

    private boolean isProductivityApp(String packageName) {
        return AppCategorizationUtils.isProductivityApp(packageName);
    }

    private String getAppType(String packageName) {
        return AppCategorizationUtils.getAppType(packageName);
    }



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

            long entBreakDuration = parentFragment.getEntertainmentBreakDuration();
            long prodBreakDuration = parentFragment.getProductivityBreakDuration();
            Log.d(TAG, "startMonitoring: Fetched from parent - Entertainment break: " +
                    (entBreakDuration/60000) + "min, Productivity break: " +
                    (prodBreakDuration/60000) + "min");


            Intent serviceIntent = new Intent(getActivity(), MonitoringService.class);
            serviceIntent.putExtra("appName", "Starting monitoring...");
            serviceIntent.putExtra("appType", "Initializing");
            serviceIntent.putExtra("startTime", System.currentTimeMillis());
            serviceIntent.putExtra("workDuration", (long) 30 * 60 * 1000);
            serviceIntent.putExtra("entertainmentBreakDuration", parentFragment.getEntertainmentBreakDuration());
            serviceIntent.putExtra("productivityBreakDuration", parentFragment.getProductivityBreakDuration());
            getActivity().startForegroundService(serviceIntent);


            monitoringThread = new Thread(() -> {
                int updateCounter = 0;
                while (isMonitoring) {
                    try {
                        String foregroundApp = getCurrentForegroundApp();
                        updateCounter++;


                        if ((foregroundApp != null && !foregroundApp.equals(currentForegroundApp)) ||
                                (updateCounter >= 5)) {

                            updateCounter = 0;

                            if (foregroundApp != null && !foregroundApp.equals(currentForegroundApp)) {

                                handleAppSwitch(foregroundApp);


                                currentForegroundApp = foregroundApp;
                                appUsageStartTime = System.currentTimeMillis();
                            }


                            if (foregroundApp != null) {

                                long workDuration = getWorkDurationForApp(foregroundApp);

                                Intent updateIntent = new Intent(getActivity(), MonitoringService.class);
                                updateIntent.setAction("UPDATE_NOTIFICATION");
                                updateIntent.putExtra("appName", AppCategorizationUtils.getAppName(getActivity(), foregroundApp));
                                updateIntent.putExtra("appType", AppCategorizationUtils.getAppType(foregroundApp));
                                updateIntent.putExtra("startTime", appUsageStartTime);
                                updateIntent.putExtra("workDuration", workDuration);
                                updateIntent.putExtra("entertainmentBreakDuration", parentFragment.getEntertainmentBreakDuration());
                                updateIntent.putExtra("productivityBreakDuration", parentFragment.getProductivityBreakDuration());
                                getActivity().startService(updateIntent);


                                updateUI(foregroundApp);


                                checkForBreakTime(foregroundApp);
                            }
                        }

                        Thread.sleep(1000);
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


            Intent serviceIntent = new Intent(getActivity(), MonitoringService.class);
            getActivity().stopService(serviceIntent);
        }
    }

    private String getCurrentForegroundApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - 5000;

            String currentApp = null;
            UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();


            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);

                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    currentApp = event.getPackageName();
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND &&
                        event.getPackageName().equals(currentApp)) {
                    currentApp = null;
                }
            }


            if (currentApp != null && currentApp.equals(getActivity().getPackageName())) {
                return null;
            }

            return currentApp;
        }
        return null;
    }

    private void handleAppSwitch(String newApp) {
        if (newApp == null) return;

        currentForegroundApp = newApp;
        appUsageStartTime = System.currentTimeMillis();
        isInBreak = false;

        if (breakTimer != null) {
            breakTimer.cancel();
        }


        long workDuration = getWorkDurationForApp(newApp);


        Intent serviceIntent = new Intent(getActivity(), MonitoringService.class);
        serviceIntent.putExtra("appName", AppCategorizationUtils.getAppName(getActivity(), newApp));
        serviceIntent.putExtra("appType", AppCategorizationUtils.getAppType(newApp));
        serviceIntent.putExtra("startTime", appUsageStartTime);
        serviceIntent.putExtra("workDuration", (long)workDuration);


        serviceIntent.putExtra("entertainmentBreakDuration", parentFragment.getEntertainmentBreakDuration());
        serviceIntent.putExtra("productivityBreakDuration", parentFragment.getProductivityBreakDuration());

        getActivity().startService(serviceIntent);

        getActivity().runOnUiThread(() -> {
            textView.setText("Now using: " + AppCategorizationUtils.getAppName(getActivity(), newApp) +
                    "\nType: " + AppCategorizationUtils.getAppType(newApp));
        });
    }

    private void checkForBreakTime(String packageName) {
        Log.d(TAG, "checkForBreakTime: packageName=" + packageName + ", isInBreak=" + isInBreak);

        if (isInBreak || appUsageStartTime == 0) {
            Log.d(TAG, "checkForBreakTime: Skipping - already in break or not tracking");
            return; // Already in break or not tracking yet
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - appUsageStartTime;


        long workDuration = getWorkDurationForApp(packageName);

        Log.d(TAG, "checkForBreakTime: elapsedTime=" + (elapsedTime/1000) + "s, workDuration=" +
                (workDuration/1000) + "s, timeLeft=" + ((workDuration - elapsedTime)/1000) + "s");

        if (elapsedTime >= workDuration) {
            Log.d(TAG, "checkForBreakTime: TIME OUT! Starting break for " + packageName);
            // Time to take a break
            startBreakTime(packageName);
        }
    }

    private void startBreakTime(String packageName) {
        Log.d(TAG, "startBreakTime: Starting break for " + packageName);
        isInBreak = true;

        // Get break duration based on app type
        final long breakDuration;
        if (parentFragment != null) {
            if (isEntertainmentApp(packageName)) {
                breakDuration = parentFragment.getEntertainmentBreakDuration();
                Log.d(TAG, "startBreakTime: Using entertainment break duration: " +
                        (breakDuration/60000) + "min");
            } else if (isProductivityApp(packageName)) {
                breakDuration = parentFragment.getProductivityBreakDuration();
                Log.d(TAG, "startBreakTime: Using productivity break duration: " +
                        (breakDuration/60000) + "min");
            } else {
                // Default to entertainment settings for unknown apps
                breakDuration = parentFragment.getEntertainmentBreakDuration();
                Log.d(TAG, "startBreakTime: Using default (entertainment) break duration: " +
                        (breakDuration/60000) + "min");
            }
        } else {
            // Fallback to default values if parent is not available
            breakDuration = 5 * 60 * 1000; // 5 minutes
        }

        Log.d(TAG, "startBreakTime: Break duration set to " + (breakDuration / 1000) + " seconds");

        // Show notification
        showBreakNotification(AppCategorizationUtils.getAppName(getActivity(), packageName), breakDuration);


        Intent breakIntent = new Intent(getActivity(), MonitoringService.class);
        breakIntent.setAction("START_BREAK");
        breakIntent.putExtra("appName", AppCategorizationUtils.getAppName(getActivity(), packageName));
        breakIntent.putExtra("breakDuration", breakDuration);
        getActivity().startService(breakIntent);

        // Show alert dialog
        getActivity().runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Time for a break!")
                    .setMessage("You've been using " + AppCategorizationUtils.getAppName(getActivity(), packageName) +
                            " for too long. Take a break for " + (breakDuration / 60000) + " minutes.")
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


                Intent endBreakIntent = new Intent(getActivity(), MonitoringService.class);
                endBreakIntent.setAction("END_BREAK");
                endBreakIntent.putExtra("appName", AppCategorizationUtils.getAppName(getActivity(), packageName));
                endBreakIntent.putExtra("appType", AppCategorizationUtils.getAppType(packageName));
                endBreakIntent.putExtra("startTime", appUsageStartTime);
                endBreakIntent.putExtra("workDuration", getWorkDurationForApp(packageName));
                getActivity().startService(endBreakIntent);

                // Notify user that break is over
                getActivity().runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Break finished")
                            .setMessage("You can resume using " + AppCategorizationUtils.getAppName(getActivity(), packageName) + " now.")
                            .setPositiveButton("OK", null)
                            .setCancelable(false)
                            .show();

                    textView.setText("Now using: " + AppCategorizationUtils.getAppName(getActivity(), packageName) +
                            "\nType: " + AppCategorizationUtils.getAppType(packageName));
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
                textView.setText("Now using: " + AppCategorizationUtils.getAppName(getActivity(), packageName) +
                        "\nType: " + AppCategorizationUtils.getAppType(packageName) +
                        "\nTime until break: " + timeLeftStr);
            });
        }
    }

    private long getWorkDurationForApp(String packageName) {
        Log.d(TAG, "getWorkDurationForApp: Enter!");
        final long duration;
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


//    private String getAppName(String packageName) {
//        PackageManager packageManager = getActivity().getPackageManager();
//        try {
//            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
//            return packageManager.getApplicationLabel(appInfo).toString();
//        } catch (PackageManager.NameNotFoundException e) {
//            return packageName;
//        }
//    }

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


    private android.content.BroadcastReceiver timeoutReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getStringExtra("packageName");
            Log.d(TAG, "Received timeout broadcast for: " + packageName);

            if (!isInBreak && packageName != null) {
                startBreakTime(packageName);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            getActivity().registerReceiver(
                    timeoutReceiver,
                    new android.content.IntentFilter("com.example.csci3310project.TIMEOUT_ACTION"),
                    Context.RECEIVER_NOT_EXPORTED
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            getActivity().unregisterReceiver(timeoutReceiver);
        } catch (IllegalArgumentException e) {

        }
    }

}