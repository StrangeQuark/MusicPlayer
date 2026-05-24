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
import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
    List<String> artists;
    List<String> albums;
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

        artists = new ArrayList<String>();
        albums = new ArrayList<String>();

        aa = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, artists);

        lv.setAdapter(aa);
        refreshArtists();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(lv.getAdapter() == aa)
                {
                    if(position < 0 || position >= artists.size())
                        return;

                    List<String> temp = new ArrayList<String>();
                    temp.add("All songs");

                    currentArtist = artists.get(position);
                    for (int i = 0; i < MainActivity.allSongModels.size(); i++) {
                        Song song = MainActivity.allSongModels.get(i);
                        if (song.artist.equals(currentArtist) && !temp.contains(song.album))
                            temp.add(song.album);
                    }
                    Collections.sort(temp.subList(1, temp.size()), String.CASE_INSENSITIVE_ORDER);
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

                    String selectedAlbum = lv.getItemAtPosition(position).toString();
                    for(int i = 0; i < MainActivity.songsAlbumsAndArtists.size(); i++)
                    {
                        Song song = MainActivity.allSongModels.get(i);
                        if(!selectedAlbum.equals("All songs") && song.artist.equals(currentArtist) && song.album.equals(selectedAlbum))
                        {
                            tempPlaylistString.add(song.title);
                            tempPlaylistArtistString.add(song.getArtistDurationText());
                            tempPlaylist.add(song.getFile());
                            tempPlaylistTrackNumber.add(song.trackNumber);
                        }
                        if(selectedAlbum.equals("All songs") && song.artist.equals(currentArtist))
                        {
                            tempPlaylistString.add(song.title);
                            tempPlaylistArtistString.add(song.getArtistDurationText());
                            tempPlaylist.add(song.getFile());
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
            lv.post(() -> lv.setSelection(currentArtistPosition));
            b = false;
            return;
        }
        if(lv.getAdapter() == aa3) {
            lv.setAdapter(aa2);
            return;
        }
    }

    public void refreshArtists()
    {
        if(artists == null || aa == null)
            return;

        artists.clear();
        albums.clear();
        for(int i = 0; i < MainActivity.allSongModels.size(); i++)
        {
            Song song = MainActivity.allSongModels.get(i);
            if(!artists.contains(song.artist))
                artists.add(song.artist);
            if(!albums.contains(song.album))
                albums.add(song.album);
        }

        Collections.sort(artists, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(albums, String.CASE_INSENSITIVE_ORDER);
        aa.notifyDataSetChanged();
    }
}
