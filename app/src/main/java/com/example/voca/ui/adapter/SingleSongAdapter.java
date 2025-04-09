package com.example.voca.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voca.R;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.PostDTO;
import com.example.voca.service.LoadImage;

import java.util.List;

public class SingleSongAdapter extends BaseAdapter {
    private Context context;
    private List<SongDTO> songs;

    public SingleSongAdapter(Context context, List<SongDTO> songs) {
        this.context = context;
        this.songs = songs;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.sing_item_layout, parent, false);

            holder = new ViewHolder();
            holder.thumbnail = convertView.findViewById(R.id.imageSong);
            holder.title = convertView.findViewById(R.id.textSongTitle);
            holder.like_times = convertView.findViewById(R.id.like_times);
            holder.recorded_people = convertView.findViewById(R.id.recorded_people);
            holder.singButton = convertView.findViewById(R.id.sing_button);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SongDTO song = songs.get(position);
        new LoadImage(holder.thumbnail).execute(song.getThumbnail());
        holder.title.setText(song.getTitle());

        int totalLikes = 0;
        holder.like_times.setText(String.valueOf(totalLikes));

        holder.recorded_people.setText(String.valueOf(song.getRecorded_people()));

        holder.singButton.setOnClickListener(v -> {

        });

        return convertView;
    }

    public void updateData(List<PostDTO> posts, List<SongDTO> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView like_times;
        TextView recorded_people;
        Button singButton;
    }



}
