package com.example.voca.ui.management;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.ui.adapter.PostAdapter;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class UserDetailsActivity extends AppCompatActivity {
    private TextInputEditText txtUsername;
    private TextInputEditText txtEmail;
    private TextInputEditText txtCreatedAt;
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<PostDTO> postList;
    private ExoPlayer player;
    private PostBUS postBUS;
    private UserBUS userBUS;
    private UserDTO curUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_user_details);
        setClickOnNavigationButton();
        postBUS = new PostBUS();
        userBUS = new UserBUS();

        Intent intent = getIntent();
        String userId = intent.getStringExtra("user_id");

        txtUsername = findViewById(R.id.txt_username);
        txtEmail = findViewById(R.id.txt_email);
        txtCreatedAt = findViewById(R.id.txt_created_at);
        userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
            @Override
            public void onUserFetched(UserDTO user) {
                txtUsername.setText(user.getUsername());
                txtEmail.setText(user.getEmail());
                txtCreatedAt.setText(user.getCreated_at());

                ImageView avatar = findViewById(R.id.avatarImage);

                Glide.with(UserDetailsActivity.this)
                        .load(user.getAvatar())
                        .placeholder(R.drawable.ic_profile_2_24dp)
                        .error(R.drawable.ic_profile_2_24dp)
                        .into(avatar);

                curUser = user;
                loadUserPosts();
            }

            @Override
            public void onError(String error) {
                Log.d("UserProfileError", error);
            }
        });

        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        player = new ExoPlayer.Builder(this).build();
        postAdapter = new PostAdapter(postList, this, player);
        recyclerView.setAdapter(postAdapter);

//        if (createdAt != null) {
//            try {
//                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
//                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
//                Date date = inputFormat.parse(createdAt);
//                String formattedDate = outputFormat.format(date);
//                txtCreatedAt.setText(formattedDate);
//            } catch (ParseException e) {
//                e.printStackTrace();
//                txtCreatedAt.setText(createdAt);
//            }
//        }
    }

    private void setClickOnNavigationButton() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}
