package com.strangequark.musicplayer.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.strangequark.musicplayer.MainActivity;
import com.strangequark.musicplayer.MediaPlayerActivity;
import com.strangequark.musicplayer.R;
import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class ArtistsFragment extends Fragment {

    ArrayAdapter aa;
    ArrayAdapter aa2;
    ArrayAdapter aa3;
    ListView lv;
    Boolean b = false;
    String currentArtist;
    int currentArtistPosition;
    List<File> tempPlaylist;
    List<String> tempPlaylistString;
    List<String> tempPlaylistArtistString;
    List<Integer> tempPlaylistTrackNumber;

    public Boolean getBoolean()
    {
        return b;
    }
    public void setBoolean(Boolean b){this.b = b;}

    public ArtistsFragment() {
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
        return inflater.inflate(R.layout.fragment_artists, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        lv = (ListView)getView().findViewById(R.id.artistsList);

        String[] projection = {
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.Albums.ALBUM
        };

        Cursor cursor = getActivity().managedQuery(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);

        List<String> artists = new ArrayList<String>();
        List<String> artistsAndAlbums = new ArrayList<String>();
        List<String> albums = new ArrayList<String>();

        while(cursor.moveToNext()){
            if(!artists.contains(cursor.getString(0)))
                artists.add(cursor.getString(0));
            if(!artistsAndAlbums.contains(cursor.getString(0) + " - " + cursor.getString(1))) {
                artistsAndAlbums.add(cursor.getString(0) + " - " + cursor.getString(1));
                albums.add(cursor.getString(1));
            }
        }

        aa = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, artists);

        lv.setAdapter(aa);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(lv.getAdapter() == aa)
                {
                    List<String> temp = new ArrayList<String>();
                    temp.add("All songs");

                    currentArtist = artists.get(position);
                    for (int i = 0; i < artistsAndAlbums.size(); i++) {
                        if (artistsAndAlbums.get(i).contains(artists.get(position)))
                            temp.add(albums.get(i));
                    }
                    aa2 = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, temp);
                    lv.setAdapter(aa2);
                    b = true;

                    currentArtistPosition = position;
                    return;
                }
                if(lv.getAdapter() == aa2)
                {
                    tempPlaylist = new ArrayList<File>();
                    tempPlaylistString = new ArrayList<String>();
                    tempPlaylistArtistString = new ArrayList<String>();
                    tempPlaylistTrackNumber = new ArrayList<Integer>();

                    for(int i = 0; i < MainActivity.songsAlbumsAndArtists.size(); i++)
                    {
                        if(MainActivity.songsAlbumsAndArtists.get(i).contains(lv.getItemAtPosition(position).toString()) && MainActivity.songsAlbumsAndArtists.get(i).contains(currentArtist) && MainActivity.allAlbumsStrings.get(i).equals(lv.getItemAtPosition(position).toString()))
                        {
                            tempPlaylistString.add(MainActivity.allSongs.get(i));
                            tempPlaylistArtistString.add(MainActivity.allArtistsStrings.get(i));
                            tempPlaylist.add(MainActivity.allSongsFiles.get(i));
                            tempPlaylistTrackNumber.add(MainActivity.allSongsTrackNumbers.get(i));
                        }
                        if(lv.getItemAtPosition(position).toString().equals("All songs") && MainActivity.albumsAndArtists.get(i).contains(currentArtist))
                        {
                            tempPlaylistString.add(MainActivity.allSongs.get(i));
                            tempPlaylistArtistString.add(MainActivity.allArtistsStrings.get(i));
                            tempPlaylist.add(MainActivity.allSongsFiles.get(i));
                            tempPlaylistTrackNumber.add(i);
                        }
                    }

                    MainActivity.concurrentSort(tempPlaylistTrackNumber, tempPlaylist, tempPlaylistArtistString, tempPlaylistString);

                    aa3 = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, tempPlaylistString);
                    lv.setAdapter(aa3);

                    b = true;
                    return;
                }
                if(lv.getAdapter() == aa3)
                {
                    MainActivity.currentPlaylist = new ArrayList<File>(tempPlaylist);
                    MainActivity.currentPlaylistString = new ArrayList<String>(tempPlaylistString);
                    MainActivity.currentPlaylistArtistString = new ArrayList<String>(tempPlaylistArtistString);

                    if(MainActivity.mp != null)
                    {
                        MainActivity.mp.stop();
                        MainActivity.mp.release();
                        MainActivity.mp = null;
                    }
                    MainActivity.mp = MediaPlayer.create(getContext(), Uri.fromFile(MainActivity.currentPlaylist.get(position)));
                    MainActivity.mp.start();

                    MainActivity.currentSongPosition = position;
                    MainActivity.currentPlaylistArtistString.add(MainActivity.allArtistsStrings.get(position));

                    MainActivity.playButton.setImageResource(R.drawable.playbutton);

                    Intent appInfo = new Intent(getActivity(), MediaPlayerActivity.class);
                    startActivity(appInfo);
                }
            }
        });
    }

    public void goBack()
    {
        if(lv.getAdapter() == aa2) {
            lv.setAdapter(aa);
            lv.post(() -> lv.setSelection(currentArtistPosition));
            b = false;
            return;
        }
        if(lv.getAdapter() == aa3) {
            lv.setAdapter(aa2);
            return;
        }
    }
}