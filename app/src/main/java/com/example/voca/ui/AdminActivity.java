package com.example.voca.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.bus.UserBUS;
import com.example.voca.ui.management.SongAdapter;
import com.example.voca.ui.management.SongDetailsActivity;
import com.example.voca.ui.management.SongsManagementActivity;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private ListView listView;
    private List<SongDTO> songList;
    private List<PostDTO> postList;
    private SongAdapter songAdapter;
    private SongBUS songBUS;
    private PostBUS postBUS;
    private Button addSongButton;
    private SearchView searchView;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_songs);

        listView = findViewById(R.id.listViewSongs);
        addSongButton = findViewById(R.id.addSong_button);
        searchView = findViewById(R.id.searchView);
        songList = new ArrayList<>();
        songBUS = new SongBUS();
        postBUS = new PostBUS();

        fetchSongs();
        addSongButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, SongsManagementActivity.class);
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                songBUS.searchSongsByTitle(query, new SongBUS.OnSongsFetchedListener() {
                    @Override
                    public void onSongsFetched(List<SongDTO> songs) {
                        songAdapter.updateData(songs); // Cập nhật danh sách hiển thị
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false; // Không tìm kiếm khi đang nhập
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            SongDTO selectedSong = songList.get(position);

            Intent intent = new Intent(AdminActivity.this, SongDetailsActivity.class);
            intent.putExtra("song_id", selectedSong.get_id());
            intent.putExtra("youtube_id", selectedSong.getYoutube_id());
            intent.putExtra("title", selectedSong.getTitle());
            intent.putExtra("mp3_file", selectedSong.getMp3_file());
            intent.putExtra("thumbnail", selectedSong.getThumbnail());
            intent.putExtra("uploaded_by", selectedSong.getUploaded_by().getUsername()); // Nếu UserDTO có `getUsername()`
            intent.putExtra("created_at", selectedSong.getCreated_at());
            intent.putExtra("recorded_people", selectedSong.getRecorded_people());

            startActivity(intent);
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        fetchSongs(); // Gọi lại API để cập nhật danh sách bài hát
    }

    private void fetchSongs() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading songs...");
        progressDialog.setCancelable(false);
        progressDialog.show(); // Hiển thị progress dialog trước khi tải

        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                songList = songs;
                fetchPosts(); // Gọi fetchPosts() sau khi tải xong bài hát
                progressDialog.dismiss(); // Ẩn progress dialog khi hoàn tất
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss(); // Ẩn progress dialog nếu có lỗi
                Toast.makeText(AdminActivity.this, "Error fetching songs: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchPosts() {
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                postList = posts;
                songAdapter = new SongAdapter(AdminActivity.this, songList, postList);
                listView.setAdapter(songAdapter);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminActivity.this, "Error fetching posts: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
