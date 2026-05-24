package com.strangequark.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.ContentResolver;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    static public MediaPlayer mp;
    static public MediaPlayer nextMp;
    static public List<File> allSongsFiles;
    static public List<Song> allSongModels;
    static public List<Long> allSongIds;
    static public List<File> currentPlaylist;
    static public List<Integer> allSongsTrackNumbers;
    static public int currentSongPosition;
    static public List<String> allSongs;
    static public List<String> currentPlaylistString;
    static public List<String> allArtistsStrings;
    static public List<String> allArtistNames;
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
    static public Map<Long, Song> songsById;
    static public Map<String, Song> songsByPath;
    static public boolean libraryLoaded = false;
    static public boolean libraryLoading = false;
    static private AudioManager sharedAudioManager;
    static private int nextSongPosition = -1;
    static private File nextSongFile;
    static private MediaPlayer.OnCompletionListener playbackCompletionListener;
    static private final AudioManager.OnAudioFocusChangeListener AUDIO_FOCUS_LISTENER = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if(mp == null)
                return;

            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS)
            {
                if (mp.isPlaying())
                    pausePlayback();
            }
        }
    };
    PlaylistsFragment playlistsFragment;
    ArtistsFragment artistsFragment;
    AlbumsFragment albumsFragment;
    SongsFragment songsFragment;
    MainActivity ma = this;
    Button button;
    AudioManager audioManager;
    ExecutorService libraryExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        sharedAudioManager = audioManager;

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
                        pausePlayback();
                    } else {
                        startPlayback(ma.getApplicationContext());
                    }
                }
            }
        });

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if(powerManager != null)
        {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicPlayer:PlaybackWakeLock");
            wakeLock.setReferenceCounted(false);
        }

        libraryExecutor = Executors.newSingleThreadExecutor();
        shuffleBoolean = false;
        repeatOneBoolean = false;
        repeatAllBoolean = false;
        allSongsTrackNumbers = new ArrayList<Integer>();
        allSongsFiles = new ArrayList<File>();
        allSongModels = new ArrayList<Song>();
        allSongIds = new ArrayList<Long>();
        currentPlaylist = new ArrayList<File>();
        allSongs = new ArrayList<String>();
        currentPlaylistTrackNumbers = new ArrayList<Integer>();
        currentPlaylistString = new ArrayList<String>();
        allArtistsStrings = new ArrayList<String>();
        allArtistNames = new ArrayList<String>();
        allAlbumsStrings = new ArrayList<String>();
        currentPlaylistArtistString = new ArrayList<String>();
        songsAlbumsAndArtists = new ArrayList<String>();
        albumsAndArtists = new ArrayList<String>();
        songsById = new HashMap<Long, Song>();
        songsByPath = new HashMap<String, Song>();
        libraryLoaded = false;
        libraryLoading = false;
        playlistsFragment = new PlaylistsFragment();
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

        if(hasStoragePermission())
        {
            button.setVisibility(View.INVISIBLE);
            loadMusicAsync();
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void setUpTabs()
    {
        ViewPager vp = (ViewPager)findViewById(R.id.viewPager);
        TabLayout tab = (TabLayout)findViewById(R.id.tabs);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(playlistsFragment, "Playlists");
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
                    loadMusicAsync();
                }
                else
                {
                    button.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private boolean hasStoragePermission()
    {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadMusicAsync()
    {
        if(libraryLoading || libraryLoaded)
            return;

        libraryLoading = true;
        libraryExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final LibraryData data = loadMusicFromMediaStore();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        applyLibraryData(data);
                    }
                });
            }
        });
    }

    private LibraryData loadMusicFromMediaStore()
    {
        LibraryData data = new LibraryData();
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

        Cursor cursor = null;
        try
        {
            ContentResolver resolver = getContentResolver();
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

            if(cursor == null)
                return data;

            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            while(cursor.moveToNext())
            {
                String path = getCursorString(cursor, dataColumn);
                if(path.length() == 0)
                    continue;

                long id = idColumn >= 0 ? cursor.getLong(idColumn) : -1;
                String artist = getCursorString(cursor, artistColumn);
                String title = getCursorString(cursor, titleColumn);
                String album = getCursorString(cursor, albumColumn);
                int duration = getCursorInt(cursor, durationColumn);
                int track = getCursorInt(cursor, trackColumn);

                Song song = new Song(id, title, artist, album, path, duration, track);
                data.songs.add(song);
                data.songFiles.add(song.getFile());
                data.songIds.add(id);
                data.titles.add(title);
                data.artistDurationStrings.add(song.getArtistDurationText());
                data.artistNames.add(artist);
                data.albums.add(album);
                data.trackNumbers.add(track);
                data.songsAlbumsAndArtists.add(title + " - " + album + " - " + artist);
                data.albumsAndArtists.add(album + " - " + artist);
                if(id >= 0)
                    data.byId.put(id, song);
                data.byPath.put(path, song);
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if(cursor != null)
                cursor.close();
        }

        return data;
    }

    private void applyLibraryData(LibraryData data)
    {
        allSongModels.clear();
        allSongModels.addAll(data.songs);
        allSongsFiles.clear();
        allSongsFiles.addAll(data.songFiles);
        allSongIds.clear();
        allSongIds.addAll(data.songIds);
        allSongs.clear();
        allSongs.addAll(data.titles);
        allArtistsStrings.clear();
        allArtistsStrings.addAll(data.artistDurationStrings);
        allArtistNames.clear();
        allArtistNames.addAll(data.artistNames);
        allAlbumsStrings.clear();
        allAlbumsStrings.addAll(data.albums);
        allSongsTrackNumbers.clear();
        allSongsTrackNumbers.addAll(data.trackNumbers);
        songsAlbumsAndArtists.clear();
        songsAlbumsAndArtists.addAll(data.songsAlbumsAndArtists);
        albumsAndArtists.clear();
        albumsAndArtists.addAll(data.albumsAndArtists);
        songsById.clear();
        songsById.putAll(data.byId);
        songsByPath.clear();
        songsByPath.putAll(data.byPath);

        libraryLoaded = true;
        libraryLoading = false;
        refreshLibraryFragments();
    }

    private static String getCursorString(Cursor cursor, int column)
    {
        if(column < 0 || cursor.isNull(column))
            return "";
        return cursor.getString(column);
    }

    private static int getCursorInt(Cursor cursor, int column)
    {
        if(column < 0 || cursor.isNull(column))
            return 0;
        try
        {
            return cursor.getInt(column);
        }
        catch(Exception ex)
        {
            return 0;
        }
    }

    private void refreshLibraryFragments()
    {
        if(playlistsFragment != null)
            playlistsFragment.loadPlaylists();
        if(artistsFragment != null)
            artistsFragment.refreshArtists();
        if(albumsFragment != null)
            albumsFragment.refreshAlbums();
        if(songsFragment != null)
            songsFragment.refreshSongs();
    }

    public static String getSongStableKey(int position)
    {
        if(position < 0 || position >= allSongModels.size())
            return "";
        return allSongModels.get(position).getStableKey();
    }

    public static Song getSongByStableKey(String key)
    {
        if(key == null)
            return null;

        if(key.startsWith("id:"))
        {
            try
            {
                return songsById.get(Long.valueOf(key.substring(3)));
            }
            catch(NumberFormatException ex)
            {
                return null;
            }
        }

        if(key.startsWith("path:"))
            return songsByPath.get(key.substring(5));

        try
        {
            return songsById.get(Long.valueOf(key));
        }
        catch(NumberFormatException ex)
        {
            return songsByPath.get(key);
        }
    }

    public static int findFilePosition(List<File> files, File file)
    {
        if(files == null || file == null)
            return -1;

        String path = file.getAbsolutePath();
        for(int i = 0; i < files.size(); i++)
        {
            File current = files.get(i);
            if(current != null && path.equals(current.getAbsolutePath()))
                return i;
        }
        return -1;
    }

    public static void setPlaybackCompletionListener(MediaPlayer.OnCompletionListener listener)
    {
        playbackCompletionListener = listener;
        if(mp != null)
            mp.setOnCompletionListener(listener);
        if(nextMp != null)
            nextMp.setOnCompletionListener(listener);
    }

    public static boolean playTrackAt(Context context, int position)
    {
        Context playbackContext = getPlaybackContext(context);
        if(playbackContext == null || !hasPlaylistPosition(position))
            return false;

        releasePreloadedTrack();
        releaseCurrentPlayer();
        currentSongPosition = position;
        updateCurrentTrackMetadata();

        mp = MediaPlayer.create(playbackContext, Uri.fromFile(currentSongFile));
        if(mp == null)
        {
            releaseWakeLock();
            return false;
        }
        if(playbackCompletionListener != null)
            mp.setOnCompletionListener(playbackCompletionListener);

        return startPlayback(playbackContext);
    }

    public static boolean playNextTrack(Context context)
    {
        int position = getNextPlaybackPosition();
        if(position < 0)
            return false;

        if(isPreloadedTrack(position))
            return playPreloadedTrack(context, position);

        return playTrackAt(context, position);
    }

    public static boolean promotePreloadedTrack(Context context, MediaPlayer completedPlayer)
    {
        Context playbackContext = getPlaybackContext(context);
        if(playbackContext == null || nextMp == null || nextSongPosition < 0)
            return false;

        MediaPlayer oldPlayer = completedPlayer != null ? completedPlayer : mp;
        MediaPlayer promotedPlayer = nextMp;
        int promotedPosition = nextSongPosition;

        nextMp = null;
        nextSongPosition = -1;
        nextSongFile = null;

        if(oldPlayer != null && oldPlayer != promotedPlayer)
            releasePlayer(oldPlayer, false);

        mp = promotedPlayer;
        currentSongPosition = promotedPosition;
        updateCurrentTrackMetadata();
        if(playbackCompletionListener != null)
            mp.setOnCompletionListener(playbackCompletionListener);

        try
        {
            if(!mp.isPlaying())
                mp.start();
        }
        catch(IllegalStateException ex)
        {
            releaseWakeLock();
            return false;
        }

        acquireWakeLock(playbackContext);
        updatePlaybackButtons(true);
        preloadNextTrack(playbackContext);
        MediaPlayerActivity.refreshNotificationOnly(playbackContext);
        return true;
    }

    public static int getNextPlaybackPosition()
    {
        if(repeatOneBoolean || currentPlaylist == null || currentPlaylist.size() == 0 ||
                currentSongPosition < 0 || currentSongPosition >= currentPlaylist.size())
            return -1;

        if(currentSongPosition < currentPlaylist.size() - 1)
            return currentSongPosition + 1;

        if(repeatAllBoolean)
            return 0;

        return -1;
    }

    public static void preloadNextTrack(Context context)
    {
        Context playbackContext = getPlaybackContext(context);
        if(playbackContext == null || mp == null)
        {
            releasePreloadedTrack();
            return;
        }

        int position = getNextPlaybackPosition();
        if(position < 0)
        {
            releasePreloadedTrack();
            return;
        }

        if(isPreloadedTrack(position))
        {
            attachPreloadedTrack();
            return;
        }

        releasePreloadedTrack();
        if(!hasPlaylistPosition(position))
            return;

        MediaPlayer player = MediaPlayer.create(playbackContext, Uri.fromFile(currentPlaylist.get(position)));
        if(player == null)
            return;

        if(playbackCompletionListener != null)
            player.setOnCompletionListener(playbackCompletionListener);

        nextMp = player;
        nextSongPosition = position;
        nextSongFile = currentPlaylist.get(position);
        attachPreloadedTrack();
    }

    public static void releasePreloadedTrack()
    {
        detachPreloadedTrack();
        if(nextMp != null)
            releasePlayer(nextMp, false);
        nextMp = null;
        nextSongPosition = -1;
        nextSongFile = null;
    }

    private static boolean playPreloadedTrack(Context context, int position)
    {
        Context playbackContext = getPlaybackContext(context);
        if(playbackContext == null || nextMp == null)
            return false;

        MediaPlayer oldPlayer = mp;
        MediaPlayer promotedPlayer = nextMp;
        nextMp = null;
        nextSongPosition = -1;
        nextSongFile = null;

        detachPreloadedTrack(oldPlayer);
        if(oldPlayer != null)
            releasePlayer(oldPlayer, true);

        mp = promotedPlayer;
        currentSongPosition = position;
        updateCurrentTrackMetadata();
        if(playbackCompletionListener != null)
            mp.setOnCompletionListener(playbackCompletionListener);

        return startPlayback(playbackContext);
    }

    private static void updateCurrentTrackMetadata()
    {
        if(!hasPlaylistPosition(currentSongPosition))
            return;

        currentSongFile = currentPlaylist.get(currentSongPosition);
        currentSongString = currentPlaylistString.get(currentSongPosition);
        currentArtistString = currentPlaylistArtistString.get(currentSongPosition);
    }

    private static boolean hasPlaylistPosition(int position)
    {
        return currentPlaylist != null &&
                currentPlaylistString != null &&
                currentPlaylistArtistString != null &&
                currentPlaylist.size() > 0 &&
                currentPlaylistString.size() == currentPlaylist.size() &&
                currentPlaylistArtistString.size() == currentPlaylist.size() &&
                position >= 0 &&
                position < currentPlaylist.size();
    }

    private static boolean isPreloadedTrack(int position)
    {
        if(nextMp == null || nextSongFile == null || !hasPlaylistPosition(position) ||
                nextSongPosition != position)
            return false;

        File requestedFile = currentPlaylist.get(position);
        return requestedFile != null &&
                requestedFile.getAbsolutePath().equals(nextSongFile.getAbsolutePath());
    }

    private static void attachPreloadedTrack()
    {
        if(mp == null || nextMp == null)
            return;

        try
        {
            mp.setNextMediaPlayer(nextMp);
        }
        catch(IllegalStateException ex)
        {
            releasePreloadedTrack();
        }
        catch(IllegalArgumentException ex)
        {
            releasePreloadedTrack();
        }
    }

    private static void detachPreloadedTrack()
    {
        detachPreloadedTrack(mp);
    }

    private static void detachPreloadedTrack(MediaPlayer player)
    {
        if(player == null)
            return;

        try
        {
            player.setNextMediaPlayer(null);
        }
        catch(IllegalStateException ex)
        {
            // The player may already be completed or released; the preload is still safe to release.
        }
        catch(IllegalArgumentException ex)
        {
            // Some platform implementations reject clearing here after completion.
        }
    }

    private static void releaseCurrentPlayer()
    {
        if(mp != null)
            releasePlayer(mp, true);
        mp = null;
    }

    private static void releasePlayer(MediaPlayer player, boolean stopFirst)
    {
        if(player == null)
            return;

        try
        {
            player.setOnCompletionListener(null);
            player.setNextMediaPlayer(null);
        }
        catch(IllegalStateException ex)
        {
            // Continue releasing below.
        }
        catch(IllegalArgumentException ex)
        {
            // Continue releasing below.
        }

        if(stopFirst)
        {
            try
            {
                player.stop();
            }
            catch(IllegalStateException ex)
            {
                // Already stopped or completed.
            }
        }

        player.release();
    }

    private static Context getPlaybackContext(Context context)
    {
        if(context == null)
            return null;
        return context.getApplicationContext();
    }

    public static void acquireWakeLock(Context context)
    {
        try
        {
            if(wakeLock == null && context != null)
            {
                PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                if(powerManager != null)
                {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicPlayer:PlaybackWakeLock");
                    wakeLock.setReferenceCounted(false);
                }
            }
            if(wakeLock != null && !wakeLock.isHeld())
                wakeLock.acquire();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void releaseWakeLock()
    {
        try
        {
            if(wakeLock != null && wakeLock.isHeld())
                wakeLock.release();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static boolean requestPlaybackFocus()
    {
        if(sharedAudioManager == null)
            return false;

        int result = sharedAudioManager.requestAudioFocus(
                AUDIO_FOCUS_LISTENER,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public static boolean startPlayback(Context context)
    {
        if(mp == null)
            return false;

        if(sharedAudioManager == null && context != null)
            sharedAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        if(!requestPlaybackFocus())
            return false;

        mp.start();
        acquireWakeLock(context);
        updatePlaybackButtons(true);
        preloadNextTrack(context);
        MediaPlayerActivity.refreshNotificationOnly(context);
        return true;
    }

    public static void abandonPlaybackFocus()
    {
        if(sharedAudioManager != null)
            sharedAudioManager.abandonAudioFocus(AUDIO_FOCUS_LISTENER);
    }

    public static void pausePlayback()
    {
        if(mp == null)
            return;

        if(mp.isPlaying())
            mp.pause();
        abandonPlaybackFocus();
        releaseWakeLock();
        updatePlaybackButtons(false);
        if(playButton != null)
            MediaPlayerActivity.refreshNotificationOnly(playButton.getContext());
    }

    public static void updatePlaybackButtons(boolean playing)
    {
        int icon = playing ? R.drawable.playbutton : R.drawable.pausebutton;
        if(playButton != null)
            playButton.setImageResource(icon);
        if(MediaPlayerActivity.playButton != null)
            MediaPlayerActivity.playButton.setImageResource(icon);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        abandonPlaybackFocus();
        if(libraryExecutor != null)
            libraryExecutor.shutdownNow();
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

    private static class LibraryData
    {
        List<Song> songs = new ArrayList<Song>();
        List<File> songFiles = new ArrayList<File>();
        List<Long> songIds = new ArrayList<Long>();
        List<String> titles = new ArrayList<String>();
        List<String> artistDurationStrings = new ArrayList<String>();
        List<String> artistNames = new ArrayList<String>();
        List<String> albums = new ArrayList<String>();
        List<Integer> trackNumbers = new ArrayList<Integer>();
        List<String> songsAlbumsAndArtists = new ArrayList<String>();
        List<String> albumsAndArtists = new ArrayList<String>();
        Map<Long, Song> byId = new HashMap<Long, Song>();
        Map<String, Song> byPath = new HashMap<String, Song>();
    }
}
