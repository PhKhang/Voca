package com.example.voca.ui; // Thay bằng package của bạn

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.ui.management.SongsManagementActivity;
import com.example.voca.ui.management.UsersManagementActivity;

import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private TextView userCount;
    private TextView postCount;
    private TextView songCount;
    private TextView playCount;
    private TextView roomCount;
    private TextView greetingTitle;
    private CardView cardUserManagement;
    private CardView cardSongManagement;
    private SongBUS songBUS;
    private UserBUS userBUS;
    private PostBUS postBUS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_admin);

        userCount = findViewById(R.id.userCount);
        postCount = findViewById(R.id.postCount);
        songCount = findViewById(R.id.songCount);
        playCount = findViewById(R.id.playCount);
        roomCount = findViewById(R.id.roomCount);

        greetingTitle = findViewById(R.id.greetingTitle);

        cardUserManagement = findViewById(R.id.cardUserManagement);
        cardSongManagement = findViewById(R.id.cardSongManagement);

        songBUS = new SongBUS();
        userBUS = new UserBUS();
        postBUS = new PostBUS();

        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        if (username != null) {
            greetingTitle.setText("Xin chào " + username + "!");
        }
        updateCountValues();

        setupCardClickListeners();
    }

    private void updateCountValues() {
        userBUS.fetchUsers(new UserBUS.OnUsersFetchedListener() {
            @Override
            public void onUsersFetched(List<UserDTO> users) {
                int totalUsers = users.size();
                userCount.setText(String.valueOf(totalUsers));
            }

            @Override
            public void onError(String error) {
                userCount.setText("0");
                Toast.makeText(AdminActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                int totalPosts = posts.size();
                postCount.setText(String.valueOf(totalPosts));
            }

            @Override
            public void onError(String error) {
                postCount.setText("0");
                Toast.makeText(AdminActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                int totalSongs = songs.size();
                songCount.setText(String.valueOf(totalSongs));

                int totalRecordedPeople = 0;
                for (SongDTO song : songs) {
                    totalRecordedPeople += song.getRecorded_people();
                }
                playCount.setText(String.valueOf(totalRecordedPeople));
            }

            @Override
            public void onError(String error) {
                songCount.setText("0");
                playCount.setText("0");
                Toast.makeText(AdminActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        roomCount.setText("0");
    }

    private void setupCardClickListeners() {
        cardUserManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, UsersManagementActivity.class);
                startActivity(intent);
            }
        });

        cardSongManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, SongsManagementActivity.class);
                startActivity(intent);
            }
        });
    }
}