package com.example.voca.ui.sing;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.media.MediaPlayer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voca.R;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.PostDTO;
import com.example.voca.service.LoadImage;
import com.example.voca.ui.management.SongAdapter;
import com.example.voca.ui.record.RecordActivity;

import java.io.IOException;
import java.util.List;

public class SingAdapter extends BaseAdapter {
    private Context context;
    private List<SongDTO> songs;
    private List<PostDTO> posts;

    public SingAdapter(Context context, List<PostDTO> posts, List<SongDTO> songs) {
        this.context = context;
        this.songs = songs;
        this.posts = posts;
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
        for (PostDTO post : posts) {
            if (post == null || post.getSong_id() == null || post.getSong_id().get_id() == null) {
                continue;
            }

            if (song.get_id() != null && post.getSong_id().get_id().equals(song.get_id())) {
                totalLikes += post.getLikes();
            }
        }
        holder.like_times.setText(String.valueOf(totalLikes));

        holder.recorded_people.setText(String.valueOf(song.getRecorded_people()));

        holder.singButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, RecordActivity.class);
            intent.putExtra("song_name", song.getTitle());
            intent.putExtra("youtube_id", song.getYoutube_id());
            intent.putExtra("mp3_file", song.getMp3_file());
            context.startActivity(intent);
        });

        return convertView;
    }

    public void updateData(List<PostDTO> posts, List<SongDTO> songs) {
        this.posts = posts;
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
