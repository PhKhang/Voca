package com.example.voca.ui.management;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voca.R;
import com.example.voca.dto.SongDTO;
import com.example.voca.service.LoadImage;

import java.util.List;

public class SongAdapter extends ArrayAdapter<SongDTO> {
    private Context context;
    private List<SongDTO> songs;

    public SongAdapter(Context context, List<SongDTO> songs) {
        super(context, R.layout.song_item_layout, songs);
        this.context = context;
        this.songs = songs;
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
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SongDTO song = songs.get(position);

        new LoadImage(holder.thumbnail).execute(song.getThumbnail());

        holder.title.setText(song.getTitle());
        holder.uploader.setText(song.getUploaded_by().getUsername()); // Giả sử UserDTO có getUsername()

        return convertView;
    }

    private class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView uploader;
    }
}
