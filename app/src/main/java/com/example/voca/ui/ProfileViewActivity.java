package com.example.voca.ui;

import android.os.Bundle;

import android.util.Log;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewActivity extends AppCompatActivity {
    private UserDTO curUser;
    RecyclerView recyclerView;
    PostAdapter postAdapter;
    List<PostDTO> postList;
    ExoPlayer player;
    PostBUS postBUS = new PostBUS();
    UserBUS userBUS = new UserBUS();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);
        getSupportActionBar().hide();

        String userId = getIntent().getStringExtra("user_id");
        player = new ExoPlayer.Builder(this).build();

        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(new ArrayList<>(), this, player);
        recyclerView.setAdapter(postAdapter);
        setClickOnNavigationButton();

        userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
            @Override
            public void onUserFetched(UserDTO user) {
                TextView username = findViewById(R.id.txt_username);
                username.setText(user.getUsername());

                ImageView avatar = findViewById(R.id.avatarImage);

                Glide.with(ProfileViewActivity.this)
                        .load(user.getAvatar())
                        .placeholder(R.drawable.ic_profile_2_24dp) // Ảnh mặc định nếu tải chậm
                        .error(R.drawable.ic_profile_2_24dp) // Ảnh nếu lỗi tải
                        .into(avatar);

                curUser = user;
                loadUserPosts();
            }

            @Override
            public void onError(String error) {
                Log.d("UserProfileError", error);
            }
        });
    }

    private void setClickOnNavigationButton() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadUserPosts() {
        postBUS.fetchPostsByUserId(curUser.get_id(), new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                postList = posts;
                postAdapter.updateData(postList);
            }

            @Override
            public void onError(String error) {
                Log.d("UserProfileError", error);
            }
        });


    }
}

