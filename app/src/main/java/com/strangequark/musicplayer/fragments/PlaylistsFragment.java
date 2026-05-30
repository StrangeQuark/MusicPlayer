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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlaylistsFragment extends Fragment {

    private static final String LEGACY_PLAYLIST_VERSION = "#playlist-v2";
    private static final String LEGACY_PLAYLIST_FILE = "playlists.txt";
    private static final String PLAYLIST_FILE = "playlists.json";
    private static final int PLAYLIST_SCHEMA_VERSION = 3;
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
            JSONObject root = new JSONObject();
            JSONArray playlists = new JSONArray();
            root.put("version", PLAYLIST_SCHEMA_VERSION);
            root.put("playlists", playlists);

            for (int i = 0; i < allPlaylists.size(); i++)
            {
                JSONObject playlist = new JSONObject();
                JSONArray songs = new JSONArray();
                playlist.put("name", allPlaylistsNames.get(i));

                for (int j = 0; j < allPlaylists.get(i).size(); j++)
                    songs.put(allPlaylists.get(i).get(j));

                playlist.put("songs", songs);
                playlists.put(playlist);
            }

            FileOutputStream fos = new FileOutputStream(new File(appContext.getFilesDir(), PLAYLIST_FILE));
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer.write(root.toString(2));
            writer.flush();
            fos.getFD().sync();
            writer.close();
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
            File filesDir = getContext().getApplicationContext().getFilesDir();
            File playlistFile = new File(filesDir, PLAYLIST_FILE);
            if(playlistFile.exists())
            {
                loadJsonPlaylists(playlistFile);
            }
            else
            {
                File legacyFile = new File(filesDir, LEGACY_PLAYLIST_FILE);
                boolean shouldSaveJson = loadLegacyPlaylists(legacyFile);
                if(shouldSaveJson)
                    savePlaylists(getContext());
            }

            aa.notifyDataSetChanged();
        }catch(Exception ex){ex.printStackTrace();}
    }

    private void loadJsonPlaylists(File file) throws Exception
    {
        String json = readTextFile(file);
        JSONObject root = new JSONObject(json);
        JSONArray playlists = root.optJSONArray("playlists");
        if(playlists == null)
            return;

        for(int i = 0; i < playlists.length(); i++)
        {
            JSONObject playlist = playlists.optJSONObject(i);
            if(playlist == null)
                continue;

            allPlaylistsNames.add(playlist.optString("name", ""));
            List<String> songs = new ArrayList<String>();
            JSONArray songKeys = playlist.optJSONArray("songs");
            if(songKeys != null)
            {
                for(int j = 0; j < songKeys.length(); j++)
                    songs.add(songKeys.optString(j, ""));
            }
            allPlaylists.add(songs);
        }
    }

    private boolean loadLegacyPlaylists(File file) throws Exception
    {
        if(!file.exists())
            return false;

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        try
        {
            String s = reader.readLine();
            boolean isV2 = LEGACY_PLAYLIST_VERSION.equals(s);
            boolean shouldSaveJson = isV2 || MainActivity.libraryLoaded;

            if(isV2)
                s = reader.readLine();

            while(s != null)
            {
                List<String> temp = new ArrayList<String>();

                String[] strings = s.split(",", -1);
                if(strings.length == 0)
                {
                    s = reader.readLine();
                    continue;
                }

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

            return shouldSaveJson;
        }
        finally
        {
            reader.close();
        }
    }

    private static String readTextFile(File file) throws Exception
    {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        try
        {
            String line = reader.readLine();
            while(line != null)
            {
                builder.append(line).append('\n');
                line = reader.readLine();
            }
        }
        finally
        {
            reader.close();
        }
        return builder.toString();
    }
}
