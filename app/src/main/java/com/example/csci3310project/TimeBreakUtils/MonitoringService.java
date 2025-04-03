package com.example.csci3310project.TimeBreakUtils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MonitoringService extends Service {

    private static final String CHANNEL_ID = "monitoring_channel";
    private static final int NOTIFICATION_ID = 1;

    private String currentAppName = "Unknown";
    private String currentAppType = "Unknown";
    private long startTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // 檢查是否是更新通知的意圖
            if ("UPDATE_NOTIFICATION".equals(intent.getAction())) {
                String appName = intent.getStringExtra("appName");
                String appType = intent.getStringExtra("appType");
                long time = intent.getLongExtra("startTime", SystemClock.elapsedRealtime());
                updateNotification(appName, appType, time);
                return START_NOT_STICKY;
            }

            // 一般啟動服務
            currentAppName = intent.getStringExtra("appName");
            currentAppType = intent.getStringExtra("appType");
            startTime = intent.getLongExtra("startTime", SystemClock.elapsedRealtime());
        }

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification().build());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
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

    private NotificationCompat.Builder createNotification() {
        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        String timeElapsed = String.format("%02d:%02d",
                (elapsedTime / 1000) / 60,
                (elapsedTime / 1000) % 60);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Monitoring App Usage")
                .setContentText("App: " + currentAppName + " | Type: " + currentAppType + " | Time: " + timeElapsed)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
    }

    public void updateNotification(String appName, String appType, long startTime) {
        this.currentAppName = appName;
        this.currentAppType = appType;
        this.startTime = startTime;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification().build());
        }
    }
}