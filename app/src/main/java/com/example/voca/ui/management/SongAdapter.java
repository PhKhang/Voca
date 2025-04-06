package com.example.voca.ui.management;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voca.R;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.PostDTO;
import com.example.voca.service.LoadImage;

import java.util.List;

public class SongAdapter extends ArrayAdapter<SongDTO> {
    private Context context;
    private List<SongDTO> songs;
    private List<PostDTO> posts;

    public SongAdapter(Context context, List<SongDTO> songs, List<PostDTO> posts) {
        super(context, R.layout.song_item_layout, songs);
        this.context = context;
        this.songs = songs;
        this.posts = posts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.song_item_layout, parent, false);

            holder = new ViewHolder();
            holder.thumbnail = convertView.findViewById(R.id.imageSong);
            holder.title = convertView.findViewById(R.id.textSongTitle);
            holder.uploader = convertView.findViewById(R.id.textUploader);
            holder.like_times = convertView.findViewById(R.id.like_times);
            holder.recorded_people = convertView.findViewById(R.id.recorded_people);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SongDTO song = songs.get(position);
        if (song == null) {
            Log.e("SongAdapter", "SongDTO at position " + position + " is null");
            return convertView; // Tránh crash
        }
        new LoadImage(holder.thumbnail).execute(song.getThumbnail());
        holder.title.setText(song.getTitle());
        holder.uploader.setText(song.getUploaded_by().getUsername());
        holder.recorded_people.setText(String.valueOf(song.getRecorded_people()));

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

        return convertView;
    }

    public void updateData(List<SongDTO> newSongs) {
        songs.clear();
        songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public void updateDataPost(List<PostDTO> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    private class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView uploader;
        TextView like_times;
        TextView recorded_people;
    }
}
