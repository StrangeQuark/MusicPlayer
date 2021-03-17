package com.strangequark.musicplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.strangequark.musicplayer.fragments.PlaylistsFragment;
import com.strangequark.musicplayer.fragments.adapters.SongListAdapter;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDialog extends AppCompatDialogFragment {
    PlaylistsFragment pf;

    public PlaylistDialog(PlaylistsFragment pf)
    {
        this.pf = pf;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        EditText editText = new EditText(this.getContext());
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setTitle("New playlist")
                .setView(editText)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String s = editText.getText().toString();
                        pf.addPlaylist(s);
                        PlaylistsFragment.savePlaylists();
                        //pf.loadPlaylists();
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
