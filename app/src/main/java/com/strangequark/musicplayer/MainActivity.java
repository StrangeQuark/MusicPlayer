package com.strangequark.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.strangequark.musicplayer.fragments.AlbumsFragment;
import com.strangequark.musicplayer.fragments.ArtistsFragment;
import com.strangequark.musicplayer.fragments.PlaylistsFragment;
import com.strangequark.musicplayer.fragments.SongsFragment;
import com.strangequark.musicplayer.fragments.adapters.ViewPagerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener
{
    static public MediaPlayer mp;
    static public List<File> allSongsFiles;
    static public List<File> currentPlaylist;
    static public List<Integer> allSongsTrackNumbers;
    static public int currentSongPosition;
    static public List<String> allSongs;
    static public List<String> currentPlaylistString;
    static public List<String> allArtistsStrings;
    static public List<String> allAlbumsStrings;
    static public List<Integer> currentPlaylistTrackNumbers;
    static public List<String> currentPlaylistArtistString;
    static public List<String> songsAlbumsAndArtists;
    static public List<String> albumsAndArtists;
    static public ListView liv;
    static public boolean shuffleBoolean = false;
    static public boolean repeatOneBoolean = false;
    static public boolean repeatAllBoolean = false;
    static public List<String> sortedStrings;
    static public List<File> sortedFiles;
    static public List<String> sortedArtistStrings;
    static public String currentArtistString;
    static public String currentSongString;
    static public File currentSongFile;
    static public int sortedPosition;
    static public ImageButton playButton;
    static public PowerManager.WakeLock wakeLock;
    ArtistsFragment artistsFragment;
    AlbumsFragment albumsFragment;
    SongsFragment songsFragment;
    MainActivity ma = this;
    Button button;
    AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myWakeLock");
        wakeLock.acquire();

        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        {
            System.out.println("ERROR IN REQUESTING AUDIO MANAGER");
            finish();
        }

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        button = (Button)findViewById(R.id.permissionsButton);
        button.setVisibility(View.INVISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(getIntent());
                Intent dialogIntent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
            }
        });

        playButton = (ImageButton)findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mp != null)
                {
                    if (mp.isPlaying())
                    {
                        mp.pause();
                        playButton.setImageResource(R.drawable.pausebutton);
                        MediaPlayerActivity.playButton.setImageResource(R.drawable.pausebutton);
                    } else {
                        mp.start();
                        playButton.setImageResource(R.drawable.playbutton);
                        MediaPlayerActivity.playButton.setImageResource(R.drawable.playbutton);
                    }
                }
            }
        });

        shuffleBoolean = false;
        repeatOneBoolean = false;
        repeatAllBoolean = false;
        allSongsTrackNumbers = new ArrayList<Integer>();
        allSongsFiles = new ArrayList<File>();
        currentPlaylist = new ArrayList<File>();
        allSongs = new ArrayList<String>();
        currentPlaylistTrackNumbers = new ArrayList<Integer>();
        currentPlaylistString = new ArrayList<String>();
        allArtistsStrings = new ArrayList<String>();
        allAlbumsStrings = new ArrayList<String>();
        currentPlaylistArtistString = new ArrayList<String>();
        songsAlbumsAndArtists = new ArrayList<String>();
        albumsAndArtists = new ArrayList<String>();
        artistsFragment = new ArtistsFragment();
        albumsFragment = new AlbumsFragment();
        songsFragment = new SongsFragment();
        liv = (ListView)findViewById(R.id.currList);

        liv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent appInfo = new Intent(ma, MediaPlayerActivity.class);
                startActivity(appInfo);
            }
        });

        setUpTabs();
    }

    private void setUpTabs()
    {
        ViewPager vp = (ViewPager)findViewById(R.id.viewPager);
        TabLayout tab = (TabLayout)findViewById(R.id.tabs);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new PlaylistsFragment(), "Playlists");
        adapter.addFragment(artistsFragment, "Artists");
        adapter.addFragment(albumsFragment, "Albums");
        adapter.addFragment(songsFragment, "Songs");

        vp.setAdapter(adapter);
        tab.setupWithViewPager(vp);

        tab.getTabAt(0).setText("Playlist");
        tab.getTabAt(1).setText("Artists");
        tab.getTabAt(2).setText("Albums");
        tab.getTabAt(3).setText("Songs");
    }

    @Override
    public void onBackPressed() {
        if(artistsFragment.getBoolean()) {
            artistsFragment.goBack();
            return;
        }
        if(albumsFragment.getBoolean()) {
            albumsFragment.goBack();
            return;
        }
        this.moveTaskToBack(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    button.setVisibility(View.INVISIBLE);

                    String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

                    String[] projection = {
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.TRACK
                    };

                    Cursor cursor = this.managedQuery(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            selection,
                            null,
                            MediaStore.Audio.Media.DEFAULT_SORT_ORDER);


                    while(cursor.moveToNext())
                    {
                        allSongs.add(cursor.getString(2));

                        allSongsTrackNumbers.add(Integer.parseInt(cursor.getString(7)));

                        int totalTime = Integer.parseInt(cursor.getString(5));
                        int minutes = (totalTime / (1000*60)) % 60;
                        int seconds = (totalTime / 1000) % 60;
                        String secondsStr = String.format("%02d", seconds);

                        allArtistsStrings.add(cursor.getString(1) + " · " + minutes + ":" + secondsStr);
                        allSongsFiles.add(new File(cursor.getString(3)));

                        songsAlbumsAndArtists.add(cursor.getString(2) + " - " + cursor.getString(6) + " - " + cursor.getString(1));
                        albumsAndArtists.add(cursor.getString(6) + " - " + cursor.getString(1));
                        allAlbumsStrings.add(cursor.getString(6));
                    }
                }
                else
                {
                    button.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if(MainActivity.mp != null)
        {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            {
                if (MainActivity.mp.isPlaying())
                    playButton.performClick();
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
            {
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
            {
                if (MainActivity.mp.isPlaying())
                    playButton.performClick();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Sort lists concurrently, this code was found at https://stackoverflow.com/questions/12164795/how-to-sort-multiple-arrays-in-java//
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static <T extends Comparable<T>> void concurrentSort(final List<T> key, List<?>... lists)
    {
        // Create a List of indices
        List<Integer> indices = new ArrayList<Integer>();
        for(int i = 0; i < key.size(); i++)
            indices.add(i);

        // Sort the indices list based on the key
        Collections.sort(indices, new Comparator<Integer>(){
            @Override public int compare(Integer i, Integer j) {
                return key.get(i).compareTo(key.get(j));
            }
        });

        // Create a mapping that allows sorting of the List by N swaps.
        // Only swaps can be used since we do not know the type of the lists
        Map<Integer,Integer> swapMap = new HashMap<Integer, Integer>(indices.size());
        List<Integer> swapFrom = new ArrayList<Integer>(indices.size()),
                swapTo   = new ArrayList<Integer>(indices.size());
        for(int i = 0; i < key.size(); i++){
            int k = indices.get(i);
            while(i != k && swapMap.containsKey(k))
                k = swapMap.get(k);

            swapFrom.add(i);
            swapTo.add(k);
            swapMap.put(i, k);
        }

        // use the swap order to sort each list by swapping elements
        for(List<?> list : lists)
            for(int i = 0; i < list.size(); i++)
                Collections.swap(list, swapFrom.get(i), swapTo.get(i));
    }
}