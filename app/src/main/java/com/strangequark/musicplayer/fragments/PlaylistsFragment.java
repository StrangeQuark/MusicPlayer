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
import com.strangequark.musicplayer.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    PlaylistsFragment pf = this;
    Button newPlaylistButton;
    ListView playistListView;

    private static Context context;
    public static List<List> allPlaylists;
    public static List<String> allPlaylistsNames;
    public static ArrayAdapter aa;

    public PlaylistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        PlaylistsFragment.context = pf.getContext();
    }

    public static Context getAppContext()
    {
        return PlaylistsFragment.context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        allPlaylists = new ArrayList<List>();
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
        List<Integer> newPlaylist = new ArrayList<>();

        allPlaylists.add(newPlaylist);
        allPlaylistsNames.add(s);

        aa.notifyDataSetChanged();
    }

    public static void savePlaylists()
    {
        try
        {
            File file = new File(PlaylistsFragment.context.getFilesDir(), "playlists.txt");

            FileOutputStream fos = new FileOutputStream(new File(PlaylistsFragment.context.getFilesDir(), "playlists.txt"));
            FileWriter fw = new FileWriter(fos.getFD());

            for (int i = 0; i < allPlaylists.size(); i++)
            {
                fw.write(allPlaylistsNames.get(i));

                for (int j = 0; j < allPlaylists.get(i).size(); j++)
                {
                    fw.write(("," + allPlaylists.get(i).get(j)));
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
        try
        {
            File file = new File(PlaylistsFragment.context.getFilesDir(), "playlists.txt");

            BufferedReader reader = new BufferedReader(new FileReader(file));

            int i = 0;

            String s = reader.readLine();

            while(s != null)
            {
                System.out.println(s);

                List<Integer> temp = new ArrayList<Integer>();

                String[] strings = s.split(",");

                allPlaylistsNames.add(strings[0]);

                for(int j = 1; j < strings.length; j++)
                {
                    temp.add(Integer.valueOf(strings[j]));
                }

                allPlaylists.add(temp);

                i++;
                s = reader.readLine();
            }

            reader.close();

            aa.notifyDataSetChanged();
        }catch(Exception ex){ex.printStackTrace();}
    }
}