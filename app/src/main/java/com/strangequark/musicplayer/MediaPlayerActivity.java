package com.strangequark.musicplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MediaPlayerActivity extends Activity{

    static public ImageButton playButton;
    ImageButton previousButton;
    ImageButton nextButton;
    ImageButton repeatButton;
    ImageButton shuffleButton;
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

    private GestureDetector gdt;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mediaplayerview);

        gdt = new GestureDetector(new GestureListener());

        textClock = (TextView)findViewById(R.id.textClock);
        textTotal = (TextView)findViewById(R.id.textTotal);
        albumArt = (ImageView)findViewById(R.id.imageView);

        albumArt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            } });

        currentArtist = new ArrayList<String>();
        currentSong = new ArrayList<String>();
        aa = new SongListAdapter(this, currentSong, currentArtist);
        aa2 = new SongListAdapter(this, MainActivity.currentPlaylistString, MainActivity.currentPlaylistArtistString);
        MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);

        cl = new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(MainActivity.repeatOneBoolean)
                {
                    MainActivity.mp.seekTo(0);
                    playButton.setImageResource(R.drawable.playbutton);
                    MainActivity.playButton.setImageResource(R.drawable.playbutton);
                    MainActivity.mp.start();
                    MainActivity.mp.setOnCompletionListener(cl);
                    return;
                }
                if(MainActivity.currentSongPosition < MainActivity.currentPlaylist.size() - 1) {
                    MainActivity.mp.stop();
                    MainActivity.mp.release();

                    MainActivity.currentSongPosition++;

                    MainActivity.mp = MediaPlayer.create(mpa, Uri.fromFile(MainActivity.currentPlaylist.get(MainActivity.currentSongPosition)));
                    MainActivity.mp.start();
                    MainActivity.mp.setOnCompletionListener(cl);

                    playButton.setImageResource(R.drawable.playbutton);
                    MainActivity.playButton.setImageResource(R.drawable.playbutton);

                    resetDisplaySong();

                    seekBar.setMax(MainActivity.mp.getDuration());

                    MainActivity.currentSongString = MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);
                }
                else {
                    if(MainActivity.repeatAllBoolean)
                    {
                        MainActivity.mp.stop();
                        MainActivity.mp.release();

                        MainActivity.currentSongPosition = 0;

                        MainActivity.mp = MediaPlayer.create(mpa, Uri.fromFile(MainActivity.currentPlaylist.get(MainActivity.currentSongPosition)));
                        MainActivity.mp.start();
                        MainActivity.mp.setOnCompletionListener(cl);

                        playButton.setImageResource(R.drawable.playbutton);
                        MainActivity.playButton.setImageResource(R.drawable.playbutton);

                        resetDisplaySong();

                        seekBar.setMax(MainActivity.mp.getDuration());

                        MainActivity.currentSongString = MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);
                    }
                    else {
                        MainActivity.mp.seekTo(0);
                        MainActivity.mp.pause();
                        playButton.setImageResource(R.drawable.pausebutton);
                        MainActivity.playButton.setImageResource(R.drawable.pausebutton);
                        MainActivity.mp.setOnCompletionListener(cl);
                    }
                }

                MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);
                updateAlbumImage(MainActivity.currentSongFile);
                updateNotification();
            }
        };

        MainActivity.mp.setOnCompletionListener(cl);

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
                if(MainActivity.mp.isPlaying())
                {
                    MainActivity.mp.pause();
                    playButton.setImageResource(R.drawable.pausebutton);
                    MainActivity.playButton.setImageResource(R.drawable.pausebutton);
                }
                else {
                    MainActivity.mp.start();
                    playButton.setImageResource(R.drawable.playbutton);
                    MainActivity.playButton.setImageResource(R.drawable.playbutton);
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
                if(MainActivity.mp.getCurrentPosition() / 1000 >= 3)
                    MainActivity.mp.seekTo(0);
                else
                {
                    if(MainActivity.currentSongPosition > 0)
                    {
                        MainActivity.mp.stop();
                        MainActivity.mp.release();

                        MainActivity.currentSongPosition--;

                        MainActivity.mp = MediaPlayer.create(mpa, Uri.fromFile(MainActivity.currentPlaylist.get(MainActivity.currentSongPosition)));
                        MainActivity.mp.start();
                        MainActivity.mp.setOnCompletionListener(cl);

                        playButton.setImageResource(R.drawable.playbutton);
                        MainActivity.playButton.setImageResource(R.drawable.playbutton);

                        resetDisplaySong();

                        seekBar.setMax(MainActivity.mp.getDuration());

                        MainActivity.currentSongString = MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);

                        MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);
                        updateAlbumImage(MainActivity.currentSongFile);
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
                if(MainActivity.currentSongPosition < MainActivity.currentPlaylist.size() - 1)
                {
                    MainActivity.mp.stop();
                    MainActivity.mp.release();

                    MainActivity.currentSongPosition++;
                    MainActivity.mp = MediaPlayer.create(mpa, Uri.fromFile(MainActivity.currentPlaylist.get(MainActivity.currentSongPosition)));
                    MainActivity.mp.start();
                    MainActivity.mp.setOnCompletionListener(cl);

                    playButton.setImageResource(R.drawable.playbutton);
                    MainActivity.playButton.setImageResource(R.drawable.playbutton);

                    resetDisplaySong();

                    seekBar.setMax(MainActivity.mp.getDuration());

                    MainActivity.currentSongString = MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);
                }
                else {
                    if(MainActivity.repeatAllBoolean)
                    {
                        MainActivity.mp.stop();
                        MainActivity.mp.release();

                        MainActivity.currentSongPosition = 0;

                        MainActivity.mp = MediaPlayer.create(mpa, Uri.fromFile(MainActivity.currentPlaylist.get(MainActivity.currentSongPosition)));
                        MainActivity.mp.start();
                        MainActivity.mp.setOnCompletionListener(cl);

                        playButton.setImageResource(R.drawable.playbutton);
                        MainActivity.playButton.setImageResource(R.drawable.playbutton);

                        resetDisplaySong();

                        seekBar.setMax(MainActivity.mp.getDuration());

                        MainActivity.currentSongString = MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);
                    }
                    else {
                        MainActivity.mp.seekTo(0);
                        playButton.performClick();
                    }
                }

                MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);
                updateAlbumImage(MainActivity.currentSongFile);
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
                    return;
                }
                if(MainActivity.repeatOneBoolean)
                {
                    repeatButton.setImageResource(R.drawable.repeatall);
                    MainActivity.repeatOneBoolean = false;
                    MainActivity.repeatAllBoolean = true;
                    return;
                }
                MainActivity.repeatOneBoolean = true;
                repeatButton.setImageResource(R.drawable.repeatone);
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
                    MainActivity.currentSongPosition = MainActivity.currentPlaylistString.indexOf(MainActivity.currentSongString);

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
                        if (MainActivity.mp != null) {
                            MainActivity.mp.stop();
                            MainActivity.mp.release();
                            MainActivity.mp = null;
                        }
                        MainActivity.mp = MediaPlayer.create(mpa, Uri.fromFile(MainActivity.currentPlaylist.get(position)));
                        MainActivity.mp.start();
                        MainActivity.mp.setOnCompletionListener(cl);

                        MainActivity.currentSongPosition = position;

                        MainActivity.currentSongString = MainActivity.currentPlaylistString.get(position);

                        playButton.setImageResource(R.drawable.playbutton);
                        MainActivity.playButton.setImageResource(R.drawable.playbutton);

                        resetDisplaySong();

                        seekBar.setMax(MainActivity.mp.getDuration());

                        updateNotification();
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
        seekBar.setProgress(MainActivity.mp.getCurrentPosition());
        currentPosition = MainActivity.mp.getCurrentPosition() / 1000;
        handler.postDelayed(r, 50);
    }

    private void updateSongTimersText()
    {
        int time = MainActivity.mp.getDuration();
        int seconds = (int) (time / 1000) % 60 ;
        int minutes = (int) ((time / (1000*60)) % 60);
        String secondsStr = String.format("%02d", seconds);
        textTotal.setText(minutes + ":" + secondsStr);

        time = MainActivity.mp.getCurrentPosition();
        seconds = (int) (time / 1000) % 60 ;
        minutes = (int) ((time / (1000*60)) % 60);
        secondsStr = String.format("%02d", seconds);
        textClock.setText(minutes + ":" + secondsStr);
    }

    //Notification
    private void addNotification()
    {
        mBuilder = new NotificationCompat.Builder(this.getApplicationContext(), "notify_001");
        Intent ii = new Intent(this.getApplicationContext(), MediaPlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, ii, 0);

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(currentArtist.get(0));
        bigText.setBigContentTitle(currentSong.get(0));

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(currentSong.get(0));
        mBuilder.setContentText("Your text");
        mBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        mBuilder.setStyle(bigText);
        mBuilder.setOngoing(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "6549456";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title 6549456", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setVibrationPattern(new long[]{0L});
            channel.enableVibration(true);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(1);
            channel.setShowBadge(false);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        notification = mBuilder.build();

        Intent intent = new Intent(this, MyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
        else
        {
            startService(intent);
        }
    }

    private void doShuffle()
    {
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

        listView.post(() -> listView.setSelection(MainActivity.currentSongPosition));
    }

    private void updateNotification()
    {
        bigText.bigText(currentArtist.get(0));
        bigText.setBigContentTitle(currentSong.get(0));
        mBuilder.setContentTitle(currentSong.get(0));

        mNotificationManager.notify(1, mBuilder.build());
    }

    private void updateAlbumImage(File f)
    {
        String[] okFileExtensions = new String[] {
                "jpg",
                "png",
                "gif",
                "jpeg"
        };

        File[] filesList = f.getAbsoluteFile().getParentFile().listFiles();
        for(File file : filesList)
        {
            for (String extension: okFileExtensions)
            {
                if (file.getName().toLowerCase().endsWith(extension))
                {
                    Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    albumArt.setImageBitmap(myBitmap);
                    return;
                }
                else {
                    albumArt.setImageResource(R.drawable.missing_album_art);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(listView.getAdapter() == aa2)
        {
            listView.setAdapter(aa);
            albumArt.setVisibility(View.VISIBLE);
            return;
        }
        super.onBackPressed();
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