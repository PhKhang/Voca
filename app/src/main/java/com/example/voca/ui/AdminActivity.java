package com.example.voca.ui;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.SongBUS;
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
    private SongAdapter songAdapter;
    private SongBUS songBUS;
    private Button addSongButton;
    private SearchView searchView;

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

        fetchSongs();
        addSongButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, SongsManagementActivity.class);
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                String searchText = query;
//                // Call the  api
//                https://wahstatus.com/wp-json/wp/v2/posts/?search= searchText + &per_page=29
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
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

            startActivity(intent);
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        fetchSongs(); // Gọi lại API để cập nhật danh sách bài hát
    }

    private void fetchSongs() {
        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                songList = songs;
                songAdapter = new SongAdapter(AdminActivity.this, songList);
                listView.setAdapter(songAdapter);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
