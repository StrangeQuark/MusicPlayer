package com.strangequark.musicplayer;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

public class MyService extends Service
{
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        startForeground(1, MediaPlayerActivity.notification);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        if(MediaPlayerActivity.mNotificationManager != null)
            MediaPlayerActivity.mNotificationManager.cancelAll();
        MainActivity.releaseWakeLock();
        this.stopSelf();
        int id = android.os.Process.myPid();
        android.os.Process.killProcess(id);
    }
}
