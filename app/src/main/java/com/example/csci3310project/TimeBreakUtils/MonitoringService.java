package com.example.csci3310project.TimeBreakUtils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.TimeUnit;

public class MonitoringService extends Service {

    private static final String CHANNEL_ID = "monitoring_channel";
    private static final int NOTIFICATION_ID = 1;

    private String currentAppName = "Unknown";
    private String currentAppType = "Unknown";
    private long startTime = 0;
    private long workDuration = 30 * 60 * 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateNotificationRunnable = new Runnable() {
        @Override
        public void run() {

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotification().build());
            }

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }


    private final Runnable checkTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isInBreak) {

                long elapsedBreakTime = System.currentTimeMillis() - breakStartTime;
                if (elapsedBreakTime >= breakDuration) {
                    Log.d("MonitoringService", "Break time finished automatically!");


                    isInBreak = false;

                    startTime = System.currentTimeMillis();


                    Intent breakEndedIntent = new Intent("com.example.csci3310project.BREAK_ENDED_ACTION");
                    breakEndedIntent.putExtra("appName", currentAppName);
                    sendBroadcast(breakEndedIntent);


                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.notify(NOTIFICATION_ID, createNotification().build());
                    }
                }
            }else {

                long elapsedTime = System.currentTimeMillis() - startTime;

                if (elapsedTime >= workDuration) {
                    Log.d("MonitoringService", "TIMEOUT DETECTED! Starting break for " + currentAppName);


                    long breakTime;
                    if (AppCategorizationUtils.isEntertainmentApp(currentAppName) ||
                            currentAppType.equals("Entertainment") ||
                            currentAppType.contains("Other")) {
                        breakTime = entertainmentBreakDuration;
                    } else {
                        breakTime = productivityBreakDuration;
                    }

                    Log.d("MonitoringService", "Using break duration: " + (breakTime/1000) + "s based on app type: " + currentAppType);


                    startBreak(currentAppName, breakTime);


                    Intent timeoutIntent = new Intent("com.example.csci3310project.TIMEOUT_ACTION");
                    timeoutIntent.putExtra("packageName", currentAppName);
                    sendBroadcast(timeoutIntent);
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    private long entertainmentBreakDuration = 10 * 60 * 1000;
    private long productivityBreakDuration = 30 * 60 * 1000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d("MonitoringService", "onStartCommand: Received intent with action: " + (action != null ? action : "null"));


            if ("START_BREAK".equals(action)) {
                Log.d("MonitoringService", "onStartCommand: Starting break");
                String appName = intent.getStringExtra("appName");
                long breakDur = intent.getLongExtra("breakDuration", 5 * 60 * 1000);
                Log.d("MonitoringService", "onStartCommand: Received break duration: " +
                        (breakDur/60000) + "min for app: " + appName);
                startBreak(appName, breakDur);
                return START_NOT_STICKY;
            }


            if ("END_BREAK".equals(action)) {
                Log.d("MonitoringService", "onStartCommand: Ending break");
                String appName = intent.getStringExtra("appName");
                String appType = intent.getStringExtra("appType");
                long startT = intent.getLongExtra("startTime", System.currentTimeMillis());
                long workDur = intent.getLongExtra("workDuration", 30 * 60 * 1000);
                endBreak(appName, appType, startT, workDur);
                return START_NOT_STICKY;
            }


            if ("UPDATE_NOTIFICATION".equals(action)) {
                String appName = intent.getStringExtra("appName");
                String appType = intent.getStringExtra("appType");
                long time = intent.getLongExtra("startTime", SystemClock.elapsedRealtime());
                long duration = intent.getLongExtra("workDuration", 30 * 60 * 1000);
                updateNotification(appName, appType, time, duration);
                return START_NOT_STICKY;
            }


            entertainmentBreakDuration = intent.getLongExtra("entertainmentBreakDuration", 10 * 60 * 1000);
            productivityBreakDuration = intent.getLongExtra("productivityBreakDuration", 30 * 60 * 1000);
            long entBreakDuration = intent.getLongExtra("entertainmentBreakDuration", 10 * 60 * 1000);
            long prodBreakDuration = intent.getLongExtra("productivityBreakDuration", 30 * 60 * 1000);

            Log.d("MonitoringService", "Before update - Entertainment break: " +
                    (entertainmentBreakDuration/60000) + "min, Productivity break: " +
                    (productivityBreakDuration/60000) + "min");

            entertainmentBreakDuration = entBreakDuration;
            productivityBreakDuration = prodBreakDuration;

            Log.d("MonitoringService", "After update - Entertainment break: " +
                    (entertainmentBreakDuration/60000) + "min, Productivity break: " +
                    (productivityBreakDuration/60000) + "min");


            String packageName = intent.getStringExtra("packageName");
            if (packageName != null) {
                currentAppName = AppCategorizationUtils.getAppName(this, packageName);
                currentAppType = AppCategorizationUtils.getAppType(packageName);
            } else {
                currentAppName = intent.getStringExtra("appName");
                currentAppType = intent.getStringExtra("appType");
            }
            startTime = intent.getLongExtra("startTime", SystemClock.elapsedRealtime());
            workDuration = intent.getLongExtra("workDuration", 30 * 60 * 1000);
        }

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification().build());
        handler.removeCallbacks(updateNotificationRunnable);
        handler.post(updateNotificationRunnable);
        handler.removeCallbacks(checkTimeoutRunnable);
        handler.post(checkTimeoutRunnable);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(updateNotificationRunnable);
        handler.removeCallbacks(checkTimeoutRunnable);
        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Displays the current app usage details");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }


    private boolean isInBreak = false;
    private long breakStartTime = 0;
    private long breakDuration = 0;


    private NotificationCompat.Builder createNotification() {
        Log.d("MonitoringService", "createNotification: isInBreak=" + isInBreak);

        if (isInBreak) {
            long elapsedBreakTime = System.currentTimeMillis() - breakStartTime;
            long breakTimeLeft = breakDuration - elapsedBreakTime;
            if (breakTimeLeft < 0) breakTimeLeft = 0;

            String timeLeftFormatted = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(breakTimeLeft),
                    (TimeUnit.MILLISECONDS.toSeconds(breakTimeLeft) % 60));

            Log.d("MonitoringService", "createNotification: Creating BREAK notification with " +
                    timeLeftFormatted + " left");

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Break Time!")
                    .setContentText("App: " + currentAppName + " | Break time left: " + timeLeftFormatted)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true);
        } else {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long timeLeft = workDuration - elapsedTime;
            if (timeLeft < 0) timeLeft = 0;

            String timeLeftFormatted = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(timeLeft),
                    (TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60));

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Monitoring App Usage")
                    .setContentText("App: " + currentAppName + " | Type: " + currentAppType +
                            " | Time left: " + timeLeftFormatted)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true);
        }
    }


    public void startBreak(String appName, long breakDuration) {
        Log.d("MonitoringService", "startBreak: Setting isInBreak=true for " + appName + " with duration " + (breakDuration / 1000) + " seconds");
        this.isInBreak = true;
        this.currentAppName = appName;
        this.breakStartTime = System.currentTimeMillis();
        this.breakDuration = breakDuration;

        // 更新通知為休息狀態
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification().build());
        }
    }

    public void endBreak(String appName, String appType, long startTime, long workDuration) {
        Log.d("MonitoringService", "endBreak: Setting isInBreak=false");
        this.isInBreak = false;
        updateNotification(appName, appType, startTime, workDuration);
    }


    public void updateNotification(String appName, String appType, long startTime, long workDuration) {
        this.currentAppName = appName;
        this.currentAppType = appType;
        this.startTime = startTime;
        this.workDuration = workDuration;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification().build());
        }
    }



}