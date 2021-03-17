package com.strangequark.musicplayer.fragments.adapters;

import android.app.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.strangequark.musicplayer.R;

import java.util.List;

public class SongListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<String> maintitle;
    private final List<String> subtitle;

    public SongListAdapter(Activity context, List<String> maintitle, List<String> subtitle) {
        super(context, R.layout.songlistlayout, maintitle);

        this.context=context;
        this.maintitle=maintitle;
        this.subtitle=subtitle;

    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.songlistlayout, null,true);

        TextView titleText = (TextView) rowView.findViewById(R.id.title);
        TextView subtitleText = (TextView) rowView.findViewById(R.id.subtitle);

        titleText.setText(maintitle.get(position));
        subtitleText.setText(subtitle.get(position));

        return rowView;

    };
}
