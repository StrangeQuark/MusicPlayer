package com.strangequark.musicplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.strangequark.musicplayer.fragments.PlaylistsFragment;

public class DeletePlaylistDialog extends AppCompatDialogFragment {
    int current;
    PlaylistActivity pa;

    public DeletePlaylistDialog(int current, PlaylistActivity pa)
    {
        this.current = current;
        this.pa = pa;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete playlist?")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PlaylistsFragment.allPlaylists.remove(current);
                        PlaylistsFragment.allPlaylistsNames.remove(current);
                        PlaylistsFragment.aa.notifyDataSetChanged();

                        pa.finish();

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
