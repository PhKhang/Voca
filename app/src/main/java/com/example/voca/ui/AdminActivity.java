package com.example.voca.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.bus.UserBUS;
import com.example.voca.ui.management.SongAdapter;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends Activity {
    private ListView listView;
    private List<SongDTO> songList;
    private SongAdapter songAdapter;
    private SongBUS songBUS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs);

        // Khởi tạo ListView và các thành phần
        listView = findViewById(R.id.listViewSongs);
        songList = new ArrayList<>();
        songBUS = new SongBUS();

        // Gọi hàm lấy dữ liệu
        fetchSongs();
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
