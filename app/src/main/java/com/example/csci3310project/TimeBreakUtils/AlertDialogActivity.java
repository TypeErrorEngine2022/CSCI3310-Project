package com.example.csci3310project.TimeBreakUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class AlertDialogActivity extends Activity {

    private static final String TAG = "AlertDialogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(android.R.style.Theme_Translucent_NoTitleBar);


        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");
        boolean playSound = getIntent().getBooleanExtra("playSound", true);


        if (playSound) {
            playNotificationSound();
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .setOnDismissListener(dialog -> finish());

        builder.show();
    }

    private void playNotificationSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mediaPlayer = MediaPlayer.create(this, soundUri);
            mediaPlayer.setVolume(0.7f, 0.7f);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> mp.release());
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }
}