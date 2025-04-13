package com.example.csci3310project.TimeBreakUtils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
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

    private static final String TAG = "MonitoringService";
    private static final String CHANNEL_ID = "monitoring_channel";
    private static final String ALERT_CHANNEL_ID = "break_alert_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int BREAK_ALERT_NOTIFICATION_ID = 2;
    private static final int BREAK_END_NOTIFICATION_ID = 3;

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
        createAlertNotificationChannel();
    }

    private boolean isInBreak = false;
    private long breakStartTime = 0;
    private long breakDuration = 0;
    private long entertainmentBreakDuration = 10 * 60 * 1000;
    private long productivityBreakDuration = 30 * 60 * 1000;

    private final Runnable checkTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isInBreak) {

                long elapsedBreakTime = System.currentTimeMillis() - breakStartTime;
                if (elapsedBreakTime >= breakDuration) {
                    Log.d(TAG, "Break time finished automatically!");

                    isInBreak = false;
                    startTime = System.currentTimeMillis();


                    showBreakEndedDialog(currentAppName);


                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.notify(NOTIFICATION_ID, createNotification().build());
                    }
                }
            } else {

                long elapsedTime = System.currentTimeMillis() - startTime;

                if (elapsedTime >= workDuration) {
                    Log.d(TAG, "TIMEOUT DETECTED! Starting break for " + currentAppName);


                    long breakTime;
                    if (AppCategorizationUtils.isEntertainmentApp(currentAppName) ||
                            currentAppType.equals("Entertainment") ||
                            currentAppType.contains("Other")) {
                        breakTime = entertainmentBreakDuration;
                    } else {
                        breakTime = productivityBreakDuration;
                    }

                    Log.d(TAG, "Using break duration: " + (breakTime/1000) + "s based on app type: " + currentAppType);


                    startBreak(currentAppName, breakTime);


                    showWorkEndedDialog(currentAppName, breakTime);
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "onStartCommand: Received intent with action: " + (action != null ? action : "null"));


            if ("START_BREAK".equals(action)) {
                Log.d(TAG, "onStartCommand: Starting break");
                String appName = intent.getStringExtra("appName");
                long breakDur = intent.getLongExtra("breakDuration", 5 * 60 * 1000);
                Log.d(TAG, "onStartCommand: Received break duration: " +
                        (breakDur/60000) + "min for app: " + appName);
                startBreak(appName, breakDur);


                showWorkEndedDialog(appName, breakDur);

                return START_NOT_STICKY;
            }


            if ("END_BREAK".equals(action)) {
                Log.d(TAG, "onStartCommand: Ending break");
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


            long entBreakDuration = intent.getLongExtra("entertainmentBreakDuration", 10 * 60 * 1000);
            long prodBreakDuration = intent.getLongExtra("productivityBreakDuration", 30 * 60 * 1000);

            Log.d(TAG, "Before update - Entertainment break: " +
                    (entertainmentBreakDuration/60000) + "min, Productivity break: " +
                    (productivityBreakDuration/60000) + "min");

            entertainmentBreakDuration = entBreakDuration;
            productivityBreakDuration = prodBreakDuration;

            Log.d(TAG, "After update - Entertainment break: " +
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

    private void createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Break Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("High priority alerts for breaks");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private NotificationCompat.Builder createNotification() {
        Log.d(TAG, "createNotification: isInBreak=" + isInBreak);

        if (isInBreak) {

            long elapsedBreakTime = System.currentTimeMillis() - breakStartTime;
            long breakTimeLeft = breakDuration - elapsedBreakTime;
            if (breakTimeLeft < 0) breakTimeLeft = 0;

            String timeLeftFormatted = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(breakTimeLeft),
                    (TimeUnit.MILLISECONDS.toSeconds(breakTimeLeft) % 60));

            Log.d(TAG, "createNotification: Creating BREAK notification with " +
                    timeLeftFormatted + " left");

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Break Time!")
                    .setContentText("App: " + currentAppName + " | Break time left: " + timeLeftFormatted)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
        Log.d(TAG, "startBreak: Setting isInBreak=true for " + appName + " with duration " + (breakDuration / 1000) + " seconds");
        this.isInBreak = true;
        this.currentAppName = appName;
        this.breakStartTime = System.currentTimeMillis();
        this.breakDuration = breakDuration;


        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification().build());
        }
    }


    public void endBreak(String appName, String appType, long startTime, long workDuration) {
        Log.d(TAG, "endBreak: Setting isInBreak=false");
        this.isInBreak = false;
        updateNotification(appName, appType, startTime, workDuration);


        showBreakEndedDialog(appName);
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


    private void showWorkEndedDialog(String appName, long breakDuration) {

        Intent dialogIntent = new Intent(this, AlertDialogActivity.class);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        dialogIntent.putExtra("title", "Time for a break!");
        dialogIntent.putExtra("message", "You've been using " + appName +
                " for too long. Take a break for " + (breakDuration / 60000) + " minutes.");
        dialogIntent.putExtra("playSound", true);


        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    1,
                    dialogIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    1,
                    dialogIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }


        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Time for a break!")
                .setContentText("You've been using " + appName + " for too long")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("You've been using " + appName +
                                " for too long. Take a break for " + (breakDuration / 60000) + " minutes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 300, 200, 300});


        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(BREAK_ALERT_NOTIFICATION_ID, builder.build());

        Log.d(TAG, "Showing work ended notification for " + appName);


        playNotificationSound();
    }


    private void showBreakEndedDialog(String appName) {

        Intent dialogIntent = new Intent(this, AlertDialogActivity.class);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        dialogIntent.putExtra("title", "Break finished");
        dialogIntent.putExtra("message", "You can resume using " + appName + " now.");
        dialogIntent.putExtra("playSound", true);


        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    2,
                    dialogIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    2,
                    dialogIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }


        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Break finished")
                .setContentText("You can resume using " + appName + " now.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 300, 200, 300});


        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(BREAK_END_NOTIFICATION_ID, builder.build());

        Log.d(TAG, "Showing break ended notification for " + appName);


        playNotificationSound();
    }


    private void playNotificationSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(getApplicationContext(), soundUri);
            mediaPlayer.setVolume(0.7f, 0.7f); // 適度音量
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(android.media.MediaPlayer::release);
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }
}