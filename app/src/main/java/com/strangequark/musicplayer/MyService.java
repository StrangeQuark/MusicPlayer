package com.strangequark.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MyService extends Service
{
    private BroadcastReceiver noisyAudioReceiver;
    private AudioDeviceCallback audioDeviceCallback;
    private AudioManager audioManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerAudioDisconnectHandlers();
        startForeground(1, MediaPlayerActivity.notification);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null)
        {
            MediaPlayerActivity.handleNotificationAction(getApplicationContext(), intent.getAction());
            if(MediaPlayerActivity.notification != null)
                startForeground(1, MediaPlayerActivity.notification);
        }
        return START_STICKY;
    }

    private void registerAudioDisconnectHandlers()
    {
        noisyAudioReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
                    pauseForDisconnectedOutput();
            }
        };
        registerReceiver(noisyAudioReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null)
        {
            audioDeviceCallback = new AudioDeviceCallback() {
                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                    if(removedDevices == null)
                        return;

                    for(AudioDeviceInfo device : removedDevices)
                    {
                        if(device != null && isMonitoredOutputDevice(device))
                        {
                            pauseForDisconnectedOutput();
                            return;
                        }
                    }
                }
            };
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
        }
    }

    private boolean isMonitoredOutputDevice(AudioDeviceInfo device)
    {
        if(!device.isSink())
            return false;

        switch(device.getType())
        {
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
            case AudioDeviceInfo.TYPE_USB_DEVICE:
            case AudioDeviceInfo.TYPE_USB_HEADSET:
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return true;
            default:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        device.getType() == AudioDeviceInfo.TYPE_HEARING_AID)
                    return true;
                return false;
        }
    }

    private void pauseForDisconnectedOutput()
    {
        if(MainActivity.mp == null || !MainActivity.mp.isPlaying())
            return;

        MainActivity.pausePlayback();
        MediaPlayerActivity.refreshNotificationOnly(getApplicationContext());
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

    @Override
    public void onDestroy() {
        if(noisyAudioReceiver != null)
        {
            unregisterReceiver(noisyAudioReceiver);
            noisyAudioReceiver = null;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null && audioDeviceCallback != null)
        {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
            audioDeviceCallback = null;
        }
        super.onDestroy();
    }
}
