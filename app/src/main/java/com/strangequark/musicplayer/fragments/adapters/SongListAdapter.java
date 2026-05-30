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
        ViewHolder holder;

        if(view == null)
        {
            LayoutInflater inflater=context.getLayoutInflater();
            view=inflater.inflate(R.layout.songlistlayout, parent, false);
            holder = new ViewHolder();
            holder.titleText = (TextView) view.findViewById(R.id.title);
            holder.subtitleText = (TextView) view.findViewById(R.id.subtitle);
            view.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) view.getTag();
        }

        holder.titleText.setText(maintitle.get(position));
        holder.subtitleText.setText(subtitle.get(position));

        return view;

    };

    private static class ViewHolder {
        TextView titleText;
        TextView subtitleText;
    }
}
