package com.strangequark.musicplayer.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.strangequark.musicplayer.PlaylistActivity;
import com.strangequark.musicplayer.PlaylistDialog;
import com.strangequark.musicplayer.MainActivity;
import com.strangequark.musicplayer.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    private static final String PLAYLIST_VERSION = "#playlist-v2";
    PlaylistsFragment pf = this;
    Button newPlaylistButton;
    ListView playistListView;

    public static List<List<String>> allPlaylists;
    public static List<String> allPlaylistsNames;
    public static ArrayAdapter aa;

    public PlaylistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        allPlaylists = new ArrayList<List<String>>();
        allPlaylistsNames = new ArrayList<String>();
        newPlaylistButton = (Button)getView().findViewById(R.id.newPlaylistButton);
        playistListView = (ListView)getView().findViewById(R.id.playlists);

        aa = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, allPlaylistsNames);

        playistListView.setAdapter(aa);

        newPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistDialog p = new PlaylistDialog(pf);
                p.show(getFragmentManager(), "NewPlaylist");
            }
        });

        playistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getContext(), PlaylistActivity.class);
                intent.putExtra("PlaylistIndex", position);
                startActivity(intent);
            }
        });

        loadPlaylists();
    }

    public void addPlaylist(String s)
    {
        List<String> newPlaylist = new ArrayList<String>();

        allPlaylists.add(newPlaylist);
        allPlaylistsNames.add(s);

        aa.notifyDataSetChanged();
    }

    public static void savePlaylists(Context context)
    {
        if(context == null)
            return;

        try
        {
            Context appContext = context.getApplicationContext();
            FileOutputStream fos = new FileOutputStream(new File(appContext.getFilesDir(), "playlists.txt"));
            FileWriter fw = new FileWriter(fos.getFD());

            fw.write(PLAYLIST_VERSION + "\n");

            for (int i = 0; i < allPlaylists.size(); i++)
            {
                fw.write(allPlaylistsNames.get(i));

                for (int j = 0; j < allPlaylists.get(i).size(); j++)
                {
                    fw.write("," + allPlaylists.get(i).get(j));
                }
                fw.write("\n");
            }

            fw.flush();
            fw.close();

            fos.getFD().sync();
            fos.close();
        }catch(Exception ex){ex.printStackTrace();}
    }

    public void loadPlaylists()
    {
        if(getContext() == null || aa == null)
            return;

        allPlaylists.clear();
        allPlaylistsNames.clear();

        try
        {
            File file = new File(getContext().getApplicationContext().getFilesDir(), "playlists.txt");
            if(!file.exists())
            {
                aa.notifyDataSetChanged();
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));

            String s = reader.readLine();
            boolean isV2 = PLAYLIST_VERSION.equals(s);
            boolean shouldSaveV2 = !isV2 && MainActivity.libraryLoaded;

            if(isV2)
                s = reader.readLine();

            while(s != null)
            {
                List<String> temp = new ArrayList<String>();

                String[] strings = s.split(",");

                allPlaylistsNames.add(strings[0]);

                for(int j = 1; j < strings.length; j++)
                {
                    if(isV2)
                    {
                        temp.add(strings[j]);
                    }
                    else if(MainActivity.libraryLoaded)
                    {
                        try
                        {
                            String stableKey = MainActivity.getSongStableKey(Integer.valueOf(strings[j]));
                            if(stableKey.length() > 0)
                                temp.add(stableKey);
                        }
                        catch(NumberFormatException ex)
                        {
                        }
                    }
                }

                allPlaylists.add(temp);

                s = reader.readLine();
            }

            reader.close();

            if(shouldSaveV2)
                savePlaylists(getContext());

            aa.notifyDataSetChanged();
        }catch(Exception ex){ex.printStackTrace();}
    }
}
