package com.strangequark.musicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlayerActivity extends AppCompatActivity{
    public static final String ACTION_PREVIOUS = "com.strangequark.musicplayer.action.PREVIOUS";
    public static final String ACTION_STOP = "com.strangequark.musicplayer.action.STOP";
    public static final String ACTION_TOGGLE_PLAYBACK = "com.strangequark.musicplayer.action.TOGGLE_PLAYBACK";
    public static final String ACTION_NEXT = "com.strangequark.musicplayer.action.NEXT";

    static public ImageButton playButton;
    static private MediaPlayerActivity activeInstance;
    static private MediaSessionCompat mediaSession;
    ImageButton previousButton;
    ImageButton nextButton;
    ImageButton repeatButton;
    ImageButton shuffleButton;
    ImageButton songMenuButton;
    List<String> currentSong;
    List<String> currentArtist;
    ListView listView;
    TextView textClock;
    TextView textTotal;
    ImageView albumArt;
    SeekBar seekBar;
    Handler handler;
    int currentPosition;
    MediaPlayerActivity mpa = this;
    ArrayAdapter aa;
    ArrayAdapter aa2;
    MediaPlayer.OnCompletionListener cl;
    NotificationCompat.BigTextStyle bigText;
    NotificationCompat.Builder mBuilder;
    static public NotificationManager mNotificationManager;
    static public Notification notification;
    private static final int SEEK_UPDATE_DELAY_MS = 500;
    private static final int REQUEST_POST_NOTIFICATIONS = 2;
    private static final String[] ALBUM_ART_EXTENSIONS = new String[] {
            "jpg",
            "png",
            "gif",
            "jpeg"
    };
    private static final LruCache<String, Bitmap> ALBUM_ART_CACHE = new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024) / 8) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };
    private static final Set<String> MISSING_ALBUM_ART_DIRS = Collections.synchronizedSet(new HashSet<String>());
    ExecutorService albumArtExecutor;

    private GestureDetector gdt;
    private static final int MENU_GO_TO_ARTIST = 1;
    private static final int MENU_GO_TO_ALBUM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mediaplayerview);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        activeInstance = this;
        albumArtExecutor = Executors.newSingleThreadExecutor();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        });

        gdt = new GestureDetector(new GestureListener());

        textClock = (TextView)findViewById(R.id.textClock);
        textTotal = (TextView)findViewById(R.id.textTotal);
        albumArt = (ImageView)findViewById(R.id.imageView);
        songMenuButton = (ImageButton)findViewById(R.id.songMenuButton);

        albumArt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            } });

        songMenuButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                showSongOptionsMenu(v);
            }
        });

        currentArtist = new ArrayList<String>();
        currentSong = new ArrayList<String>();
        if(!hasPlayableState())
        {
            finish();
            return;
        }

        aa = new SongListAdapter(this, currentSong, currentArtist);
        aa2 = new SongListAdapter(this, MainActivity.currentPlaylistString, MainActivity.currentPlaylistArtistString);
        MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);

        cl = new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(MainActivity.repeatOneBoolean)
                {
                    MainActivity.releasePreloadedTrack();
                    MainActivity.mp.seekTo(0);
                    if(!MainActivity.startPlayback(mpa.getApplicationContext()))
                        return;
                    return;
                }

                if(MainActivity.promotePreloadedTrack(mpa.getApplicationContext(), mp))
                {
                    refreshPlaybackUi();
                    return;
                }

                if(MainActivity.getNextPlaybackPosition() >= 0)
                {
                    if(MainActivity.playNextTrack(mpa.getApplicationContext()))
                        refreshPlaybackUi();
                    return;
                }

                MainActivity.releasePreloadedTrack();
                MainActivity.mp.seekTo(0);
                MainActivity.mp.pause();
                MainActivity.releaseWakeLock();
                MainActivity.updatePlaybackButtons(false);
                updateNotification();
            }
        };

        MainActivity.setPlaybackCompletionListener(cl);
        MainActivity.preloadNextTrack(getApplicationContext());

        //Init seekbar stuff
        handler = new Handler();
        seekBar = (SeekBar)findViewById(R.id.seekBar);

        seekBar.setMax(MainActivity.mp.getDuration());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(MainActivity.mp != null && fromUser)
                    MainActivity.mp.seekTo(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Init playbutton stuff
        playButton = (ImageButton)findViewById(R.id.pausePlayButton);
        if(MainActivity.mp.isPlaying())
            playButton.setImageResource(R.drawable.playbutton);
        else
            playButton.setImageResource(R.drawable.pausebutton);

        playButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(MainActivity.mp == null)
                    return;

                if(MainActivity.mp.isPlaying())
                {
                    MainActivity.pausePlayback();
                }
                else {
                    MainActivity.startPlayback(mpa.getApplicationContext());
                }
            }
        });

        //Init previousButton stuff
        previousButton = (ImageButton)findViewById(R.id.previousButton);
        previousButton.setImageResource(R.drawable.previousbutton);

        previousButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(MainActivity.mp == null)
                    return;

                if(MainActivity.mp.getCurrentPosition() / 1000 >= 3)
                    MainActivity.mp.seekTo(0);
                else
                {
                    if(MainActivity.currentSongPosition > 0)
                    {
                        if(!MainActivity.playTrackAt(mpa.getApplicationContext(), MainActivity.currentSongPosition - 1))
                            return;
                        refreshPlaybackUi();
                    }
                    else {
                        MainActivity.mp.seekTo(0);
                        playButton.performClick();
                    }
                }

                updateNotification();
            }
        });

        //Init nextButton stuff
        nextButton = (ImageButton)findViewById(R.id.nextButton);
        nextButton.setImageResource(R.drawable.nextbutton);

        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(MainActivity.mp == null)
                    return;

                if(MainActivity.getNextPlaybackPosition() >= 0)
                {
                    if(!MainActivity.playNextTrack(mpa.getApplicationContext()))
                        return;
                    refreshPlaybackUi();
                }
                else {
                    MainActivity.releasePreloadedTrack();
                    MainActivity.mp.seekTo(0);
                    playButton.performClick();
                }

                if(hasPlayableState())
                {
                    MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);
                    updateAlbumImage(MainActivity.currentSongFile);
                }
                updateNotification();
            }
        });

        //Init repeatButton stuff
        repeatButton = (ImageButton)findViewById(R.id.repeatButton);
        if(MainActivity.repeatAllBoolean)
            repeatButton.setImageResource(R.drawable.repeatall);
        else if(MainActivity.repeatOneBoolean)
            repeatButton.setImageResource(R.drawable.repeatone);
        else
            repeatButton.setImageResource(R.drawable.repeatoff);

        repeatButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(MainActivity.repeatAllBoolean)
                {
                    repeatButton.setImageResource(R.drawable.repeatoff);
                    MainActivity.repeatOneBoolean = false;
                    MainActivity.repeatAllBoolean = false;
                    MainActivity.preloadNextTrack(mpa.getApplicationContext());
                    return;
                }
                if(MainActivity.repeatOneBoolean)
                {
                    repeatButton.setImageResource(R.drawable.repeatall);
                    MainActivity.repeatOneBoolean = false;
                    MainActivity.repeatAllBoolean = true;
                    MainActivity.preloadNextTrack(mpa.getApplicationContext());
                    return;
                }
                MainActivity.repeatOneBoolean = true;
                repeatButton.setImageResource(R.drawable.repeatone);
                MainActivity.preloadNextTrack(mpa.getApplicationContext());
                return;
            }
        });

        //Init shuffleButton stuff
        shuffleButton = (ImageButton)findViewById(R.id.shuffleButton);
        if(MainActivity.shuffleBoolean)
            shuffleButton.setImageResource(R.drawable.shuffleon);
        else
            shuffleButton.setImageResource(R.drawable.shuffleoff);

        shuffleButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(MainActivity.shuffleBoolean)
                {
                    MainActivity.shuffleBoolean = false;
                    shuffleButton.setImageResource(R.drawable.shuffleoff);

                    MainActivity.currentPlaylist = new ArrayList<>(MainActivity.sortedFiles);
                    MainActivity.currentPlaylistString = new ArrayList<>(MainActivity.sortedStrings);
                    MainActivity.currentPlaylistArtistString = new ArrayList<>(MainActivity.sortedArtistStrings);
                    MainActivity.currentSongPosition = MainActivity.findFilePosition(MainActivity.currentPlaylist, MainActivity.currentSongFile);
                    if(MainActivity.currentSongPosition < 0)
                        MainActivity.currentSongPosition = Math.min(MainActivity.sortedPosition, MainActivity.currentPlaylist.size() - 1);
                    if(MainActivity.currentSongPosition >= 0 && MainActivity.currentSongPosition < MainActivity.currentPlaylistString.size())
                    {
                        MainActivity.currentSongString = MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);
                        MainActivity.currentArtistString = MainActivity.currentPlaylistArtistString.get(MainActivity.currentSongPosition);
                    }
                    MainActivity.preloadNextTrack(mpa.getApplicationContext());

                    if(listView.getAdapter() == aa2)
                    {
                        aa2 = new SongListAdapter(mpa, MainActivity.currentPlaylistString, MainActivity.currentPlaylistArtistString);
                        listView.setAdapter(aa2);
                        listView.post(() -> listView.setSelection(MainActivity.currentSongPosition));
                    }
                    else
                    {
                        aa2 = new SongListAdapter(mpa, MainActivity.currentPlaylistString, MainActivity.currentPlaylistArtistString);
                    }
                }
                else
                {
                    doShuffle();
                }
                return;
            }
        });

        //ListView stuff
        listView = (ListView)findViewById(R.id.songPlaying);

        currentSong.add(MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition));
        currentArtist.add(MainActivity.currentPlaylistArtistString.get(MainActivity.currentSongPosition));

        listView.setAdapter(aa);

        if(MainActivity.liv != null)
            MainActivity.liv.setAdapter(aa);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(listView.getAdapter() == aa)
                {
                    albumArt.setVisibility(View.INVISIBLE);
                    listView.setAdapter(aa2);
                    listView.post(() -> listView.setSelection(MainActivity.currentSongPosition));
                    return;
                }
                if(listView.getAdapter() == aa2)
                {
                    if(position != MainActivity.currentSongPosition)
                    {
                        if(!MainActivity.playTrackAt(mpa.getApplicationContext(), position))
                            return;
                        refreshPlaybackUi();
                    }

                    listView.setAdapter(aa);

                    MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);
                    updateAlbumImage(MainActivity.currentSongFile);
                    albumArt.setVisibility(View.VISIBLE);
                    return;
                }
            }
        });

        if(MainActivity.shuffleBoolean)
        {
            doShuffle();
        }

        updateAlbumImage(MainActivity.currentSongFile);

        r.run();
        requestNotificationPermissionIfNeeded();
        addNotification();
    }

    private void resetDisplaySong()
    {
        currentSong.remove(0);
        currentArtist.remove(0);
        currentSong.add(MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition));
        currentArtist.add(MainActivity.currentPlaylistArtistString.get(MainActivity.currentSongPosition));

        aa.notifyDataSetChanged();
    }

    //Seek bar stuff
    Runnable r = new Runnable()
    {
        @Override
        public void run()
        {
            updateSongTimersText();
            updateSeekBar();
        }
    };

    private void updateSeekBar()
    {
        if(handler == null || seekBar == null || MainActivity.mp == null)
            return;

        try
        {
            int position = MainActivity.mp.getCurrentPosition();
            seekBar.setProgress(position);
            currentPosition = position / 1000;
            handler.postDelayed(r, SEEK_UPDATE_DELAY_MS);
        }
        catch(IllegalStateException ex)
        {
            MainActivity.releaseWakeLock();
        }
    }

    private void updateSongTimersText()
    {
        if(MainActivity.mp == null)
            return;

        try
        {
            int time = MainActivity.mp.getDuration();
            int seconds = (int) (time / 1000) % 60 ;
            int minutes = (int) ((time / (1000*60)) % 60);
            String secondsStr = String.format(Locale.US, "%02d", seconds);
            textTotal.setText(minutes + ":" + secondsStr);

            time = MainActivity.mp.getCurrentPosition();
            seconds = (int) (time / 1000) % 60 ;
            minutes = (int) ((time / (1000*60)) % 60);
            secondsStr = String.format(Locale.US, "%02d", seconds);
            textClock.setText(minutes + ":" + secondsStr);
        }
        catch(IllegalStateException ex)
        {
            MainActivity.releaseWakeLock();
        }
    }

    //Notification
    private void addNotification()
    {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notification = buildNotification(this, mNotificationManager);

        Intent intent = new Intent(this, MyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
        else
        {
            startService(intent);
        }
    }

    private void requestNotificationPermissionIfNeeded()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
        }
    }

    private void showSongOptionsMenu(View anchor)
    {
        final Song song = MainActivity.getCurrentSong();
        if(song == null)
            return;

        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, MENU_GO_TO_ARTIST, 0, "Go to artist");
        menu.getMenu().add(0, MENU_GO_TO_ALBUM, 1, "Go to album");
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == MENU_GO_TO_ARTIST)
                {
                    openMainActivityNavigation(MainActivity.TARGET_ARTIST, song);
                    return true;
                }
                if(item.getItemId() == MENU_GO_TO_ALBUM)
                {
                    openMainActivityNavigation(MainActivity.TARGET_ALBUM, song);
                    return true;
                }
                return false;
            }
        });
        menu.show();
    }

    private void openMainActivityNavigation(String target, Song song)
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_OPEN_TARGET, target);
        intent.putExtra(MainActivity.EXTRA_ARTIST, song.artist);
        intent.putExtra(MainActivity.EXTRA_ALBUM, song.album);
        startActivity(intent);
    }

    private void doShuffle()
    {
        if(!hasPlayableState())
            return;

        MainActivity.shuffleBoolean = true;
        shuffleButton.setImageResource(R.drawable.shuffleon);

        MainActivity.sortedStrings = new ArrayList<>(MainActivity.currentPlaylistString);
        MainActivity.sortedFiles = new ArrayList<>(MainActivity.currentPlaylist);
        MainActivity.sortedArtistStrings = new ArrayList<>(MainActivity.currentPlaylistArtistString);
        MainActivity.sortedPosition = MainActivity.currentSongPosition;

        MainActivity.currentArtistString = MainActivity.sortedArtistStrings.get(MainActivity.sortedPosition);
        MainActivity.currentSongString = MainActivity.sortedStrings.get(MainActivity.sortedPosition);
        MainActivity.currentSongFile = MainActivity.sortedFiles.get(MainActivity.sortedPosition);

        MainActivity.currentPlaylist.remove(MainActivity.sortedPosition);
        MainActivity.currentPlaylistArtistString.remove(MainActivity.sortedPosition);
        MainActivity.currentPlaylistString.remove(MainActivity.sortedPosition);

        long seed = System.nanoTime();

        Collections.shuffle(MainActivity.currentPlaylistString, new Random(seed));
        Collections.shuffle(MainActivity.currentPlaylist, new Random(seed));
        Collections.shuffle(MainActivity.currentPlaylistArtistString, new Random(seed));

        MainActivity.currentPlaylist.add(0, MainActivity.currentSongFile);
        MainActivity.currentPlaylistArtistString.add(0, MainActivity.currentArtistString);
        MainActivity.currentPlaylistString.add(0, MainActivity.currentSongString);
        MainActivity.currentSongPosition = 0;

        aa2.notifyDataSetChanged();

        MainActivity.preloadNextTrack(mpa.getApplicationContext());
        listView.post(() -> listView.setSelection(MainActivity.currentSongPosition));
    }

    private void updateNotification()
    {
        if(mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notification = buildNotification(this, mNotificationManager);
        if(mNotificationManager != null)
            mNotificationManager.notify(1, notification);
    }

    private static Notification buildNotification(Context context, NotificationManager notificationManager)
    {
        String channelId = "6549456";
        if(notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Playback Controls", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setVibrationPattern(new long[]{0L});
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }

        syncMediaSession(context);

        Intent openIntent = new Intent(context, MediaPlayerActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | pendingIntentImmutableFlag());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext(), channelId);
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText(getCurrentArtistText(context));
        style.setBigContentTitle(getCurrentSongTitle(context));

        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        builder.setContentTitle(getCurrentSongTitle(context));
        builder.setContentText(getCurrentArtistText(context));
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setOnlyAlertOnce(true);
        builder.setShowWhen(false);
        builder.setStyle(style);
        builder.setOngoing(MainActivity.mp != null && MainActivity.mp.isPlaying());
        builder.addAction(R.drawable.previousbutton, "Previous", getServicePendingIntent(context, ACTION_PREVIOUS, 1));
        builder.addAction(R.drawable.ic_stop, "Stop", getServicePendingIntent(context, ACTION_STOP, 2));
        builder.addAction(getPlayPauseActionIcon(), getPlayPauseActionLabel(), getServicePendingIntent(context, ACTION_TOGGLE_PLAYBACK, 3));
        builder.addAction(R.drawable.nextbutton, "Next", getServicePendingIntent(context, ACTION_NEXT, 4));
        builder.setStyle(new MediaStyle()
                .setShowActionsInCompactView(0, 2, 3)
                .setMediaSession(mediaSession != null ? mediaSession.getSessionToken() : null));

        return builder.build();
    }

    private static PendingIntent getServicePendingIntent(Context context, String action, int requestCode)
    {
        Intent intent = new Intent(context, MyService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | pendingIntentImmutableFlag());
    }

    private static int pendingIntentImmutableFlag()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return PendingIntent.FLAG_IMMUTABLE;
        return 0;
    }

    private static void ensureMediaSession(Context context)
    {
        if(mediaSession == null)
        {
            mediaSession = new MediaSessionCompat(context.getApplicationContext(), "MusicPlayerSession");
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onPlay()
                {
                    performTransportAction(context.getApplicationContext(), ACTION_TOGGLE_PLAYBACK);
                }

                @Override
                public void onPause()
                {
                    performTransportAction(context.getApplicationContext(), ACTION_TOGGLE_PLAYBACK);
                }

                @Override
                public void onSkipToPrevious()
                {
                    performTransportAction(context.getApplicationContext(), ACTION_PREVIOUS);
                }

                @Override
                public void onSkipToNext()
                {
                    performTransportAction(context.getApplicationContext(), ACTION_NEXT);
                }

                @Override
                public void onStop()
                {
                    performTransportAction(context.getApplicationContext(), ACTION_STOP);
                }
            });
            mediaSession.setActive(true);
        }
    }

    private static void syncMediaSession(Context context)
    {
        ensureMediaSession(context);
        if(mediaSession == null)
            return;

        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_STOP;

        int state = PlaybackStateCompat.STATE_NONE;
        long position = 0L;
        float speed = 0f;
        if(MainActivity.mp != null)
        {
            position = safeCurrentPosition();
            if(MainActivity.mp.isPlaying())
            {
                state = PlaybackStateCompat.STATE_PLAYING;
                speed = 1f;
            }
            else
            {
                state = PlaybackStateCompat.STATE_PAUSED;
            }
        }

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, speed, SystemClock.elapsedRealtime())
                .build());

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getCurrentSongTitle(context))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getCurrentArtistText(context))
                .build());
    }

    private static int safeCurrentPosition()
    {
        if(MainActivity.mp == null)
            return 0;

        try
        {
            return MainActivity.mp.getCurrentPosition();
        }
        catch(IllegalStateException ex)
        {
            return 0;
        }
    }

    private static String getCurrentSongTitle(Context context)
    {
        if(MainActivity.currentPlaylistString != null &&
                MainActivity.currentSongPosition >= 0 &&
                MainActivity.currentSongPosition < MainActivity.currentPlaylistString.size())
            return MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);
        return context.getString(R.string.app_name);
    }

    private static String getCurrentArtistText(Context context)
    {
        if(MainActivity.currentPlaylistArtistString != null &&
                MainActivity.currentSongPosition >= 0 &&
                MainActivity.currentSongPosition < MainActivity.currentPlaylistArtistString.size())
            return MainActivity.currentPlaylistArtistString.get(MainActivity.currentSongPosition);
        return "";
    }

    private static int getPlayPauseActionIcon()
    {
        return MainActivity.mp != null && MainActivity.mp.isPlaying() ? R.drawable.playbutton : R.drawable.pausebutton;
    }

    private static String getPlayPauseActionLabel()
    {
        return MainActivity.mp != null && MainActivity.mp.isPlaying() ? "Pause" : "Play";
    }

    public static void handleNotificationAction(Context context, String action)
    {
        performTransportAction(context, action);
    }

    private void dispatchNotificationAction(String action)
    {
        if(ACTION_PREVIOUS.equals(action) && previousButton != null)
            previousButton.performClick();
        else if(ACTION_NEXT.equals(action) && nextButton != null)
            nextButton.performClick();
        else if(ACTION_TOGGLE_PLAYBACK.equals(action) && playButton != null)
            playButton.performClick();
        else if(ACTION_STOP.equals(action))
            stopPlaybackFromNotification();

        syncMediaSession(this);
        updateNotification();
    }

    private static void performTransportAction(Context context, String action)
    {
        MediaPlayerActivity instance = activeInstance;
        if(instance != null)
        {
            instance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instance.dispatchNotificationAction(action);
                }
            });
            return;
        }

        if(ACTION_TOGGLE_PLAYBACK.equals(action))
        {
            if(MainActivity.mp != null)
            {
                if(MainActivity.mp.isPlaying())
                    stopPlaybackFromNotification();
                else
                    MainActivity.startPlayback(context.getApplicationContext());
            }
        }
        else if(ACTION_STOP.equals(action))
        {
            stopPlaybackFromNotification();
        }

        syncMediaSession(context);
        refreshNotificationOnly(context);
    }

    private static void stopPlaybackFromNotification()
    {
        if(MainActivity.mp == null)
            return;

        MainActivity.pausePlayback();
        try
        {
            MainActivity.mp.seekTo(0);
        }
        catch(IllegalStateException ex)
        {
            MainActivity.releaseWakeLock();
        }

        if(activeInstance != null)
            activeInstance.refreshPlaybackUi();
    }

    public static void refreshNotificationOnly(Context context)
    {
        if(context == null)
            return;

        if(activeInstance != null)
        {
            activeInstance.updateNotification();
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager == null)
            return;

        syncMediaSession(context);
        notification = buildNotification(context.getApplicationContext(), manager);
        manager.notify(1, notification);
    }

    private void refreshPlaybackUi()
    {
        MainActivity.updatePlaybackButtons(MainActivity.mp != null && MainActivity.mp.isPlaying());

        if(currentSong != null && currentArtist != null && currentSong.size() > 0 && currentArtist.size() > 0 &&
                MainActivity.currentPlaylistString != null &&
                MainActivity.currentPlaylistArtistString != null &&
                MainActivity.currentSongPosition >= 0 &&
                MainActivity.currentSongPosition < MainActivity.currentPlaylistString.size())
        {
            currentSong.set(0, MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition));
            currentArtist.set(0, MainActivity.currentPlaylistArtistString.get(MainActivity.currentSongPosition));
            aa.notifyDataSetChanged();
        }

        if(seekBar != null && MainActivity.mp != null)
        {
            seekBar.setMax(MainActivity.mp.getDuration());
            seekBar.setProgress(MainActivity.mp.getCurrentPosition());
        }

        if(MainActivity.currentPlaylist != null &&
                MainActivity.currentSongPosition >= 0 &&
                MainActivity.currentSongPosition < MainActivity.currentPlaylist.size())
        {
            MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);
            updateAlbumImage(MainActivity.currentSongFile);
        }

        updateSongTimersText();
        updateNotification();
    }

    private void updateAlbumImage(File f)
    {
        if(albumArt == null || f == null || f.getAbsoluteFile().getParentFile() == null)
        {
            if(albumArt != null)
                albumArt.setImageResource(R.drawable.missing_album_art);
            return;
        }

        final File parent = f.getAbsoluteFile().getParentFile();
        final String directoryPath = parent.getAbsolutePath();
        final String requestedSongPath = f.getAbsolutePath();
        Bitmap cached = ALBUM_ART_CACHE.get(directoryPath);
        if(cached != null)
        {
            albumArt.setImageBitmap(cached);
            return;
        }

        albumArt.setImageResource(R.drawable.missing_album_art);
        if(MISSING_ALBUM_ART_DIRS.contains(directoryPath) || albumArtExecutor == null || albumArtExecutor.isShutdown())
            return;

        albumArtExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                File[] filesList = parent.listFiles();
                if(filesList != null)
                {
                    for(File file : filesList)
                    {
                        if(file != null && file.isFile() && isAlbumArtFile(file))
                        {
                            bitmap = decodeSampledBitmap(file.getAbsolutePath(), 512, 512);
                            if(bitmap != null)
                                break;
                        }
                    }
                }

                if(bitmap != null)
                    ALBUM_ART_CACHE.put(directoryPath, bitmap);
                else
                    MISSING_ALBUM_ART_DIRS.add(directoryPath);

                final Bitmap result = bitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isFinishing() || !isCurrentAlbumArtRequest(requestedSongPath))
                            return;

                        if(result != null)
                            albumArt.setImageBitmap(result);
                        else
                            albumArt.setImageResource(R.drawable.missing_album_art);
                    }
                });
            }
        });
    }

    private boolean isAlbumArtFile(File file)
    {
        String name = file.getName().toLowerCase(Locale.ROOT);
        for(String extension : ALBUM_ART_EXTENSIONS)
        {
            if(name.endsWith("." + extension))
                return true;
        }
        return false;
    }

    private boolean isCurrentAlbumArtRequest(String requestedSongPath)
    {
        return MainActivity.currentSongFile != null &&
                requestedSongPath.equals(MainActivity.currentSongFile.getAbsolutePath());
    }

    private Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth)
        {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth)
                inSampleSize *= 2;
        }

        return inSampleSize;
    }

    private boolean hasPlayableState()
    {
        return MainActivity.mp != null &&
                MainActivity.currentPlaylist != null &&
                MainActivity.currentPlaylistString != null &&
                MainActivity.currentPlaylistArtistString != null &&
                MainActivity.currentPlaylist.size() > 0 &&
                MainActivity.currentPlaylistString.size() == MainActivity.currentPlaylist.size() &&
                MainActivity.currentPlaylistArtistString.size() == MainActivity.currentPlaylist.size() &&
                MainActivity.currentSongPosition >= 0 &&
                MainActivity.currentSongPosition < MainActivity.currentPlaylist.size();
    }

    @Override
    protected void onDestroy() {
        if(handler != null)
            handler.removeCallbacksAndMessages(null);
        if(albumArtExecutor != null)
            albumArtExecutor.shutdownNow();
        if(activeInstance == this)
            activeInstance = null;
        super.onDestroy();
    }

    private void handleBackPressed()
    {
        if(listView != null && listView.getAdapter() == aa2)
        {
            listView.setAdapter(aa);
            albumArt.setVisibility(View.VISIBLE);
            return;
        }
        finish();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener
    {
        private static final int MIN_SWIPPING_DISTANCE = 50;
        private static final int THRESHOLD_VELOCITY = 50;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            if (e1.getX() - e2.getX() > MIN_SWIPPING_DISTANCE && Math.abs(velocityX) > THRESHOLD_VELOCITY)
            {
                nextButton.performClick();
                return false;
            }
            else if (e2.getX() - e1.getX() > MIN_SWIPPING_DISTANCE && Math.abs(velocityX) > THRESHOLD_VELOCITY)
            {
                previousButton.performClick();
                return false;
            }
            return false;
        }
    }
}
