package com.strangequark.musicplayer.fragments;

import android.content.Intent;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.strangequark.musicplayer.MainActivity;
import com.strangequark.musicplayer.MediaPlayerActivity;
import com.strangequark.musicplayer.R;
import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SongsFragment extends Fragment {

    ListView lv;
    List<String> tempSongs;
    List<String> tempArtists;
    ArrayAdapter aa;

    public SongsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        lv = (ListView)getView().findViewById(R.id.songsList);

        tempSongs = new ArrayList<String>();
        tempArtists = new ArrayList<String>();

        aa = new SongListAdapter(getActivity(), tempSongs, tempArtists);

        lv.setAdapter(aa);
        refreshSongs();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(MainActivity.allSongsFiles == null || MainActivity.allSongsFiles.size() == 0)
                    return;

                if(position == 0)
                {
                    Random r = new Random();
                    position = r.nextInt(MainActivity.allSongsFiles.size());
                    MainActivity.shuffleBoolean = true;
                }
                else
                {
                    position = position - 1;
                }

                if(MainActivity.mp != null)
                {
                    MainActivity.mp.stop();
                    MainActivity.mp.release();
                    MainActivity.mp = null;
                }
                MainActivity.mp = MediaPlayer.create(getContext(), Uri.fromFile(MainActivity.allSongsFiles.get(position)));
                if(MainActivity.mp == null)
                    return;
                MainActivity.mp.start();
                MainActivity.acquireWakeLock(getContext());

                MainActivity.currentSongPosition = position;

                MainActivity.currentPlaylist = new ArrayList<>(MainActivity.allSongsFiles);
                MainActivity.currentPlaylistString = new ArrayList<>(MainActivity.allSongs);
                MainActivity.currentPlaylistArtistString = new ArrayList<>(MainActivity.allArtistsStrings);
                MainActivity.currentSongFile = MainActivity.currentPlaylist.get(MainActivity.currentSongPosition);
                MainActivity.currentSongString = MainActivity.currentPlaylistString.get(MainActivity.currentSongPosition);

                MainActivity.playButton.setImageResource(R.drawable.playbutton);

                Intent appInfo = new Intent(getActivity(), MediaPlayerActivity.class);
                startActivity(appInfo);
            }
        });
    }

    public void refreshSongs()
    {
        if(tempSongs == null || tempArtists == null || aa == null)
            return;

        tempSongs.clear();
        tempArtists.clear();
        tempSongs.add("Shuffle all");
        tempArtists.add("");
        tempSongs.addAll(MainActivity.allSongs);
        tempArtists.addAll(MainActivity.allArtistsStrings);
        aa.notifyDataSetChanged();
    }
}
