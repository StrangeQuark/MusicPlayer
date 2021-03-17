package com.strangequark.musicplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.strangequark.musicplayer.fragments.PlaylistsFragment;
import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.util.ArrayList;
import java.util.List;

public class AddSongDialog extends AppCompatDialogFragment {
    PlaylistActivity pa;
    ListView listView;
    ArrayAdapter aa;
    List<Integer> playlist;

    public AddSongDialog(PlaylistActivity pa)
    {
        this.pa = pa;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        playlist = new ArrayList<Integer>();

        listView = new ListView(this.getContext());

        aa = new SongListAdapter(this.getActivity(), MainActivity.allSongs, MainActivity.allArtistsStrings);
        listView.setAdapter(aa);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playlist.add(position);
            }
        });

        builder.setTitle("Add songs")
                .setView(listView)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for(int i = 0; i < playlist.size(); i++)
                        {
                            int current = playlist.get(i);

                            pa.addSong(current, MainActivity.allSongs.get(current), MainActivity.allArtistsStrings.get(current), MainActivity.allSongsFiles.get(current).toString());

                            PlaylistsFragment.savePlaylists();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.out.println("Cancel");
                    }
                });
        return builder.create();
    }
}
