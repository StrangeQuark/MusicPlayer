package com.strangequark.musicplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.strangequark.musicplayer.fragments.PlaylistsFragment;

import java.io.File;
import java.util.List;

public class EditPlaylistDialog extends AppCompatDialogFragment {
    int position;
    int size;
    int current;
    boolean delete = false;
    PlaylistActivity pa;
    LinearLayout layout;

    public EditPlaylistDialog(int position, int size, PlaylistActivity pa)
    {
        this.position = position;
        this.size = size;
        this.current = position;
        this.pa = pa;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String[] items = new String[size];
        for(int i = 1; i <= size; i++)
            items[i-1] = Integer.toString(i);

        Spinner spinner = new Spinner(this.getContext());
        ArrayAdapter aa =  new ArrayAdapter(this.getContext(), android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(aa);
        spinner.setSelection(position);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                current = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button button = new Button(this.getContext());
        button.setText("Delete from playlist");
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!delete) {
                    delete = true;
                    button.setBackgroundColor(Color.RED);
                }
                else
                {
                    delete = false;
                    button.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        layout = new LinearLayout(this.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(spinner);
        layout.addView(button);

        builder.setTitle("Edit position")
                .setView(layout)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(delete)
                        {
                            pa.files.remove(position);
                            pa.songs.remove(position);
                            pa.artists.remove(position);
                            pa.currentPlaylist.remove(position);
                            pa.aa.notifyDataSetChanged();
                        }
                        else
                        {
                            File f = pa.files.get(position);
                            String s1 = pa.songs.get(position);
                            String s2 = pa.artists.get(position);
                            int j = pa.currentPlaylist.get(position);

                            pa.files.remove(position);
                            pa.songs.remove(position);
                            pa.artists.remove(position);
                            pa.currentPlaylist.remove(position);

                            pa.files.add(current, f);
                            pa.songs.add(current, s1);
                            pa.artists.add(current, s2);
                            pa.currentPlaylist.add(current, j);

                            pa.aa.notifyDataSetChanged();
                        }

                        PlaylistsFragment.savePlaylists();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        return builder.create();
    }
}
