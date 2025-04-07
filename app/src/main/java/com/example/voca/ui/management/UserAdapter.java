package com.example.voca.ui.management;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.voca.R;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.service.LoadImage;

import java.util.List;

public class UserAdapter extends ArrayAdapter<UserDTO> {
    private Context context;
    private List<UserDTO> users;
    private List<PostDTO> posts;

    public UserAdapter(Context context, List<UserDTO> users, List<PostDTO> posts) {
        super(context, R.layout.user_item_layout, users);
        this.context = context;
        this.users = users;
        this.posts = posts;
    }

    @Override
    public View getView(int position, @NonNull View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.user_item_layout, parent, false);

            holder = new ViewHolder();
            holder.avatar = convertView.findViewById(R.id.avatar);
            holder.username = convertView.findViewById(R.id.textUsername);
            holder.email = convertView.findViewById(R.id.textEmail);
            holder.postCount = convertView.findViewById(R.id.postCount);
            holder.role = convertView.findViewById(R.id.textRole);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        UserDTO user = users.get(position);
        if (user == null) {
            Log.e("UserAdapter", "UserDTO at position " + position + " is null");
            return convertView;
        }

        new LoadImage(holder.avatar).execute(user.getAvatar());

        holder.username.setText(user.getUsername());
        holder.email.setText(user.getEmail());
        holder.role.setText(user.getRole());

        int userPostCount = 0;
        for (PostDTO post : posts) {
            if (post == null || post.getUser_id() == null || post.getUser_id().get_id() == null) {
                continue;
            }

            if (user.get_id() != null && post.getUser_id().get_id().equals(user.get_id())) {
                userPostCount++;
            }
        }
        holder.postCount.setText(String.valueOf(userPostCount));

        return convertView;
    }

    public void updateData(List<UserDTO> newUsers) {
        users.clear();
        users.addAll(newUsers);
        notifyDataSetChanged();
    }

    public void updateDataPost(List<PostDTO> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView avatar;
        TextView username;
        TextView email;
        TextView postCount;
        TextView role;
    }
}