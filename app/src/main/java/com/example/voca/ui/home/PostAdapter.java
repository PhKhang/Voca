package com.example.voca.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voca.R;
import com.example.voca.dto.PostDTO;
import com.example.voca.service.LoadImage;
import com.example.voca.ui.record.RecordActivity;

import java.util.List;

public class PostAdapter extends BaseAdapter {
    private Context context;
    private List<PostDTO> posts;

    public PostAdapter(Context context, List<PostDTO> posts) {
        this.context = context;
        this.posts = posts;
    }

    @Override
    public int getCount() {
        return posts.size();
    }

    @Override
    public Object getItem(int position) {
        return posts.get(position);
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
            convertView = inflater.inflate(R.layout.post_item_layout, parent, false);

            holder = new ViewHolder();
            holder.thumbnail = convertView.findViewById(R.id.imageSong);
            holder.title = convertView.findViewById(R.id.textSongTitle);
            holder.subtitle = convertView.findViewById(R.id.textSongSubtitle);
            holder.uploader = convertView.findViewById(R.id.textUploader);
            holder.likeTimes = convertView.findViewById(R.id.like_times);
            holder.avatar = convertView.findViewById(R.id.avatar);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PostDTO post = posts.get(position);

        if (post.getSong_id() != null) {
            new LoadImage(holder.thumbnail).execute(post.getSong_id().getThumbnail());
            holder.title.setText(post.getSong_id().getTitle());
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_dashboard_black_24dp); // Ảnh mặc định
            holder.title.setText("Unknown Song");
        }

        holder.subtitle.setText(post.getCaption() != null ? post.getCaption() : "No caption");

        if (post.getUser_id() != null && post.getUser_id().getUsername() != null) {
            holder.uploader.setText(post.getUser_id().getUsername());
        } else {
            holder.uploader.setText("Unknown User");
        }

        if (post.getUser_id() != null && post.getUser_id().getAvatar() != null) {
            new LoadImage(holder.avatar).execute(post.getUser_id().getAvatar());
        } else {
            holder.avatar.setImageResource(R.drawable.ic_profile_2_24dp);
        }

        holder.likeTimes.setText(String.valueOf(post.getLikes()));

        return convertView;
    }

    static class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView subtitle;
        TextView uploader;
        TextView likeTimes;
        ImageView avatar;
    }

    public void updateData(List<PostDTO> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }
}