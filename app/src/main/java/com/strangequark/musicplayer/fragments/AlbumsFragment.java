package com.strangequark.musicplayer.fragments;

import android.content.Intent;
import android.database.Cursor;
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
import com.strangequark.musicplayer.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumsFragment extends Fragment {

    ArrayAdapter aa;
    ArrayAdapter aa2;
    Boolean b = false;
    ListView lv;
    String currentAlbum;
    String currentArtist;
    int currentAlbumPosition;
    List<String> albums;
    List<String> artists;
    List<File> tempPlaylist;
    List<String> tempPlaylistString;
    List<String> tempPlaylistArtistString;
    List<Integer> tempPlaylistTrackNumber;

    public Boolean getBoolean()
    {
        return b;
    }
    public void setBoolean(Boolean b){this.b = b;}

    public AlbumsFragment() {
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
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        lv = (ListView)getView().findViewById(R.id.albumsListView);

        albums = new ArrayList<String>();
        artists = new ArrayList<String>();

        aa = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, albums);

        lv.setAdapter(aa);
        refreshAlbums();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(lv.getAdapter() == aa)
                {
                    if(position < 0 || position >= albums.size())
                        return;

                    tempPlaylist = new ArrayList<File>();
                    tempPlaylistString = new ArrayList<String>();
                    tempPlaylistArtistString = new ArrayList<String>();
                    tempPlaylistTrackNumber = new ArrayList<Integer>();

                    currentAlbum = albums.get(position);
                    currentArtist = artists.get(position);

                    for(int i = 0; i < MainActivity.songsAlbumsAndArtists.size(); i++)
                    {
                        Song song = MainActivity.allSongModels.get(i);
                        if(song.album.equals(currentAlbum) && song.artist.equals(currentArtist))
                        {
                            tempPlaylistString.add(song.title);
                            tempPlaylistArtistString.add(song.getArtistDurationText());
                            tempPlaylist.add(song.getFile());
                            tempPlaylistTrackNumber.add(song.trackNumber);
                        }
                    }

                    MainActivity.concurrentSort(tempPlaylistTrackNumber, tempPlaylist, tempPlaylistArtistString, tempPlaylistString);

                    aa2 = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, tempPlaylistString);
                    lv.setAdapter(aa2);

                    b = true;

                    currentAlbumPosition = position;
                    return;
                }
                if(lv.getAdapter() == aa2)
                {
                    MainActivity.currentPlaylist = new ArrayList<File>(tempPlaylist);
                    MainActivity.currentPlaylistString = new ArrayList<String>(tempPlaylistString);
                    MainActivity.currentPlaylistArtistString = new ArrayList<String>(tempPlaylistArtistString);
                    MainActivity.currentPlaylistTrackNumbers = new ArrayList<Integer>(tempPlaylistTrackNumber);
                    MainActivity.currentSongPosition = position;
                    if(!MainActivity.playTrackAt(getContext(), position))
                        return;

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
            lv.post(() -> lv.setSelection(currentAlbumPosition));
            b = false;
            return;
        }
    }

    public void refreshAlbums()
    {
        if(albums == null || artists == null || aa == null)
            return;

        albums.clear();
        artists.clear();
        for(int i = 0; i < MainActivity.allSongModels.size(); i++)
        {
            Song song = MainActivity.allSongModels.get(i);
            if(!albums.contains(song.album))
            {
                albums.add(song.album);
                artists.add(song.artist);
            }
        }

        List<Integer> indices = new ArrayList<Integer>();
        for(int i = 0; i < albums.size(); i++)
            indices.add(i);

        Collections.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                int albumCompare = albums.get(left).compareToIgnoreCase(albums.get(right));
                if(albumCompare != 0)
                    return albumCompare;
                return artists.get(left).compareToIgnoreCase(artists.get(right));
            }
        });

        List<String> sortedAlbums = new ArrayList<String>();
        List<String> sortedArtists = new ArrayList<String>();
        for(Integer index : indices)
        {
            sortedAlbums.add(albums.get(index));
            sortedArtists.add(artists.get(index));
        }

        albums.clear();
        albums.addAll(sortedAlbums);
        artists.clear();
        artists.addAll(sortedArtists);
        aa.notifyDataSetChanged();
    }
}
