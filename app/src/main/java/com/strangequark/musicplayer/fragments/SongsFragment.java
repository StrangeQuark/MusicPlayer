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
        ListView lv = (ListView)getView().findViewById(R.id.songsList);

        List<String> tempSongs = new ArrayList<String>(MainActivity.allSongs);
        List<String> tempArtists = new ArrayList<String>(MainActivity.allArtistsStrings);

        tempSongs.add(0, "Shuffle all");
        tempArtists.add(0, "");

        ArrayAdapter aa = new SongListAdapter(getActivity(), tempSongs, tempArtists);

        lv.setAdapter(aa);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0)
                {
                    Random r = new Random();
                    position = r.nextInt(tempSongs.size()) + 1;
                    MainActivity.shuffleBoolean = true;
                }

                if(MainActivity.mp != null)
                {
                    MainActivity.mp.stop();
                    MainActivity.mp.release();
                    MainActivity.mp = null;
                }
                MainActivity.mp = MediaPlayer.create(getContext(), Uri.fromFile(MainActivity.allSongsFiles.get(position-1)));
                MainActivity.mp.start();

                MainActivity.currentSongPosition = position-1;

                MainActivity.currentPlaylist = new ArrayList<>(MainActivity.allSongsFiles);
                MainActivity.currentPlaylistString = new ArrayList<>(MainActivity.allSongs);
                MainActivity.currentPlaylistArtistString = new ArrayList<>(MainActivity.allArtistsStrings);

                MainActivity.playButton.setImageResource(R.drawable.playbutton);

                Intent appInfo = new Intent(getActivity(), MediaPlayerActivity.class);
                startActivity(appInfo);
            }
        });
    }
}