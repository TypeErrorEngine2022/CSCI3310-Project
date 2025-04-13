package com.example.csci3310project;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.csci3310project.TimeBreakUtils.AppUsageAdapter;
import com.example.csci3310project.TimeBreakUtils.AppUsageData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.csci3310project.TimeBreakUtils.MonitoringService;
import com.example.csci3310project.TimeBreakUtils.AppCategorizationUtils;

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


    private RecyclerView appUsageRecyclerView;
    private AppUsageAdapter appUsageAdapter;
    private List<AppUsageData> appUsageList = new ArrayList<>();
    private Map<String, Long> appUsageStartTimes = new HashMap<>();
    private int updateCounter = 0;


    private boolean isEntertainmentApp(String packageName) {
        return AppCategorizationUtils.isEntertainmentApp(packageName);
    }

    private boolean isProductivityApp(String packageName) {
        return AppCategorizationUtils.isProductivityApp(packageName);
    }


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
        textView.setText("Let's Start!");

        button = view.findViewById(R.id.button);
        button.setOnClickListener(this);


        appUsageRecyclerView = view.findViewById(R.id.appUsageRecyclerView);
        appUsageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        appUsageAdapter = new AppUsageAdapter(appUsageList);
        appUsageRecyclerView.setAdapter(appUsageAdapter);

        usageStatsManager = (UsageStatsManager) getActivity().getSystemService(Context.USAGE_STATS_SERVICE);
        AppCategorizationUtils.initRepository(getActivity());


        createNotificationChannel();


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


            if (parentFragment != null) {
                parentFragment.setMonitoringActive(true);
            }
        } else {
            stopMonitoring();
            button.setText("Start");


            if (parentFragment != null) {
                parentFragment.setMonitoringActive(false);
            }
        }
    }

    private void startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true;
            textView.setText("On monitoring ......");


            appUsageList.clear();
            appUsageStartTimes.clear();
            appUsageAdapter.notifyDataSetChanged();
            appUsageRecyclerView.setVisibility(View.VISIBLE);


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
                updateCounter = 0;
                while (isMonitoring) {
                    try {
                        String foregroundApp = getCurrentForegroundApp();
                        updateCounter++;

                        if ((foregroundApp != null && !foregroundApp.equals(currentForegroundApp)) ||
                                (updateCounter >= 5)) {
                            updateCounter = 0;

                            if (foregroundApp != null && !foregroundApp.equals(currentForegroundApp)) {

                                if (currentForegroundApp != null && !currentForegroundApp.isEmpty()) {
                                    long usageTime = System.currentTimeMillis() - appUsageStartTime;
                                    updateAppUsage(currentForegroundApp, usageTime);
                                }

                                handleAppSwitch(foregroundApp);
                            } else if (foregroundApp != null) {

                                long currentUsageTime = System.currentTimeMillis() - appUsageStartTime;
                                updateAppUsage(foregroundApp, currentUsageTime);


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
            textView.setText("Stop monitoring");


            appUsageRecyclerView.setVisibility(View.GONE);


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
        appUsageStartTimes.put(newApp, appUsageStartTime);
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
    }


    private void updateAppUsage(String packageName, long additionalTime) {
        try {
            PackageManager pm = getActivity().getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            Drawable icon = pm.getApplicationIcon(appInfo);
            String appName = AppCategorizationUtils.getAppName(getActivity(), packageName);
            String appType = AppCategorizationUtils.getAppType(packageName);


            boolean found = false;
            for (int i = 0; i < appUsageList.size(); i++) {
                AppUsageData data = appUsageList.get(i);
                if (data.getPackageName().equals(packageName)) {

                    long totalUsage = data.getUsageDuration() + additionalTime;
                    AppUsageData updatedData = new AppUsageData(
                            packageName, icon, appName, appType, totalUsage, System.currentTimeMillis()
                    );
                    appUsageList.set(i, updatedData);
                    found = true;
                    break;
                }
            }

            if (!found) {

                AppUsageData newData = new AppUsageData(
                        packageName, icon, appName, appType, additionalTime, System.currentTimeMillis()
                );
                appUsageList.add(newData);
            }


            Collections.sort(appUsageList, (a, b) -> Long.compare(b.getLastUsed(), a.getLastUsed()));


            getActivity().runOnUiThread(() -> {
                appUsageAdapter.notifyDataSetChanged();
            });

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app info", e);
        }
    }

    private void checkForBreakTime(String packageName) {
        if (isInBreak || appUsageStartTime == 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - appUsageStartTime;
        long workDuration = getWorkDurationForApp(packageName);

        if (elapsedTime >= workDuration) {
            Log.d(TAG, "checkForBreakTime: TIME OUT! Starting break for " + packageName);

            startBreakTime(packageName);
        }
    }

    private void startBreakTime(String packageName) {
        Log.d(TAG, "startBreakTime: Starting break for " + packageName);
        isInBreak = true;


        final long breakDuration;
        if (parentFragment != null) {
            if (isEntertainmentApp(packageName)) {
                breakDuration = parentFragment.getEntertainmentBreakDuration();
            } else if (isProductivityApp(packageName)) {
                breakDuration = parentFragment.getProductivityBreakDuration();
            } else {

                breakDuration = parentFragment.getEntertainmentBreakDuration();
            }
        } else {

            breakDuration = 5 * 60 * 1000;
        }


        Intent breakIntent = new Intent(getActivity(), MonitoringService.class);
        breakIntent.setAction("START_BREAK");
        breakIntent.putExtra("appName", AppCategorizationUtils.getAppName(getActivity(), packageName));
        breakIntent.putExtra("breakDuration", breakDuration);
        getActivity().startService(breakIntent);


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
                appUsageStartTime = System.currentTimeMillis();

                Intent endBreakIntent = new Intent(getActivity(), MonitoringService.class);
                endBreakIntent.setAction("END_BREAK");
                endBreakIntent.putExtra("appName", AppCategorizationUtils.getAppName(getActivity(), packageName));
                endBreakIntent.putExtra("appType", AppCategorizationUtils.getAppType(packageName));
                endBreakIntent.putExtra("startTime", appUsageStartTime);
                endBreakIntent.putExtra("workDuration", getWorkDurationForApp(packageName));
                getActivity().startService(endBreakIntent);


                getActivity().runOnUiThread(() -> {
                    textView.setText("On monitoring ......");
                });
            }
        };

        breakTimer.start();
    }

    private long getWorkDurationForApp(String packageName) {
        if (parentFragment != null) {
            if (isEntertainmentApp(packageName)) {
                return parentFragment.getEntertainmentWorkDuration();
            } else if (isProductivityApp(packageName)) {
                return parentFragment.getProductivityWorkDuration();
            } else {

                return parentFragment.getEntertainmentWorkDuration();
            }
        }

        return 30 * 60 * 1000; // 30 分鐘
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