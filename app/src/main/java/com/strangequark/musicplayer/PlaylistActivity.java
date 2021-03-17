package com.strangequark.musicplayer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.strangequark.musicplayer.fragments.PlaylistsFragment;
import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends FragmentActivity {

    PlaylistActivity pa = this;
    int currentIndex;
    ListView playlistListView;
    Button editPlaylistButton;
    Button deletePlaylistButton;
    Button finishEditingButton;
    Button addSongButton;
    List<File> files;
    List<String> songs;
    List<String> artists;
    ArrayAdapter aa;
    List<Integer> currentPlaylist;
    boolean isEditing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.playlistview);

        playlistListView = (ListView)findViewById(R.id.playlistListView);
        editPlaylistButton = (Button)findViewById(R.id.editButton);
        deletePlaylistButton = (Button)findViewById(R.id.deletePlaylistButton);
        finishEditingButton = (Button)findViewById(R.id.finishEditButton);
        addSongButton = (Button)findViewById(R.id.addSongButton);
        files = new ArrayList<File>();
        songs = new ArrayList<String>();
        artists = new ArrayList<String>();
        currentPlaylist = new ArrayList<Integer>();

        aa = new SongListAdapter(this, songs, artists);

        Intent intent = getIntent();
        currentIndex = intent.getIntExtra("PlaylistIndex", 0);

        editPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEditing = true;

                editPlaylistButton.setVisibility(View.INVISIBLE);

                deletePlaylistButton.setVisibility(View.VISIBLE);
                finishEditingButton.setVisibility(View.VISIBLE);
            }
        });

        deletePlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeletePlaylistDialog d = new DeletePlaylistDialog(currentIndex, pa);
                d.show(getSupportFragmentManager(), "DeletePlaylist");

                deletePlaylistButton.setVisibility(View.INVISIBLE);
                finishEditingButton.setVisibility(View.INVISIBLE);

                editPlaylistButton.setVisibility(View.VISIBLE);
            }
        });

        finishEditingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePlaylistButton.setVisibility(View.INVISIBLE);
                finishEditingButton.setVisibility(View.INVISIBLE);

                editPlaylistButton.setVisibility(View.VISIBLE);
            }
        });

        addSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddSongDialog p = new AddSongDialog(pa);
                p.show(getSupportFragmentManager(), "AddSong");
            }
        });

        playlistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(isEditing)
                {
                    EditPlaylistDialog e = new EditPlaylistDialog(position, files.size(), pa);
                    e.show(getSupportFragmentManager(), "EditPlaylist");
                }
                else {
                    MainActivity.currentPlaylist = new ArrayList<File>(files);
                    MainActivity.currentPlaylistString = new ArrayList<String>(songs);
                    MainActivity.currentPlaylistArtistString = new ArrayList<String>(artists);
                    if (MainActivity.mp != null) {
                        MainActivity.mp.stop();
                        MainActivity.mp.release();
                        MainActivity.mp = null;
                    }
                    MainActivity.mp = MediaPlayer.create(pa.getApplicationContext(), Uri.fromFile(MainActivity.currentPlaylist.get(position)));
                    MainActivity.mp.start();

                    MainActivity.currentSongPosition = position;
                    MainActivity.currentPlaylistArtistString.add(MainActivity.allArtistsStrings.get(position));

                    MainActivity.playButton.setImageResource(R.drawable.playbutton);

                    Intent appInfo = new Intent(pa, MediaPlayerActivity.class);
                    startActivity(appInfo);
                }
            }
        });

        playlistListView.setAdapter(aa);

        loadSongs();
    }

    public void addSong(int pos, String title, String artist, String data)
    {
        PlaylistsFragment.allPlaylists.get(currentIndex).add(pos);
        songs.add(title);
        artists.add(artist);
        files.add(new File(data));

        loadSongs();
    }

    public void loadSongs()
    {
        currentPlaylist = PlaylistsFragment.allPlaylists.get(currentIndex);
        System.out.println("Playlist size: " + currentPlaylist.size());
        for(int i = 0; i < currentPlaylist.size(); i++)
        {
            System.out.println("Current index: " + currentPlaylist.get(i));
            if(!songs.contains(MainActivity.allSongs.get(currentPlaylist.get(i))))
            {
                songs.add(MainActivity.allSongs.get(currentPlaylist.get(i)));
                artists.add(MainActivity.allArtistsStrings.get(currentPlaylist.get(i)));
                files.add(MainActivity.allSongsFiles.get(currentPlaylist.get(i)));
            }
        }

        aa.notifyDataSetChanged();
    }
}
