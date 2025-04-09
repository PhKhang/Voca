package com.example.voca.ui.management;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.ui.adapter.SongAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SongsManagementActivity extends AppCompatActivity {
    private static final String LOADING_MESSAGE = "Đang tải bài hát...";
    private ListView songListView;
    private List<SongDTO> songs;
    private List<PostDTO> posts;
    private SongAdapter songAdapter;
    private SongBUS songBUS;
    private PostBUS postBUS;
    private ExtendedFloatingActionButton addSongButton;
    private SearchView searchView;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_songs);

        initializeViews();
        initializeData();
        setupListeners();
        loadSongs();
        setClickOnNavigationButton();

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

    private void initializeViews() {
        songListView = findViewById(R.id.listViewSongs);
        addSongButton = findViewById(R.id.addSong_button);
        searchView = findViewById(R.id.searchView);
    }

    private void initializeData() {
        songs = new ArrayList<>();
        posts = new ArrayList<>();
        songBUS = new SongBUS();
        postBUS = new PostBUS();
        songAdapter = new SongAdapter(this, songs, posts);
        songListView.setAdapter(songAdapter);
    }

    private void setupListeners() {
        addSongButton.setOnClickListener(v -> navigateToSongAdd());
        setupSearchViewListener();
        setupRootViewClickListener();
        setupSongItemClickListener();
    }

    private void setupSearchViewListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    songAdapter.updateData(songs);
                } else {
                    searchSongsByTitle(newText);
                }
                return true;
            }
        });
    }

    private void setupRootViewClickListener() {
        findViewById(R.id.root_layout).setOnClickListener(v -> {
            searchView.clearFocus();
            searchView.setQuery("", false);
            songAdapter.updateData(songs);
        });
    }

    private void setupSongItemClickListener() {
        songListView.setOnItemClickListener((parent, view, position, id) -> {
            SongDTO selectedSong = songAdapter.getItem(position);
            navigateToSongDetails(selectedSong);
        });
    }

    private void navigateToSongAdd() {
        Intent intent = new Intent(this, SongAddActivity.class);
        startActivity(intent);
    }

    private void navigateToSongDetails(SongDTO song) {
        Intent intent = new Intent(this, SongDetailsActivity.class);
        intent.putExtra("song_id", song.get_id());
        intent.putExtra("youtube_id", song.getYoutube_id());
        intent.putExtra("title", song.getTitle());
        intent.putExtra("mp3_file", song.getMp3_file());
        intent.putExtra("thumbnail", song.getThumbnail());
        intent.putExtra("uploaded_by", song.getUploaded_by().getUsername());
        intent.putExtra("created_at", song.getCreated_at());
        intent.putExtra("recorded_people", song.getRecorded_people());
        startActivity(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadSongs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchView.setQuery("", false);
        findViewById(R.id.root_layout).requestFocus();
    }

    private void loadSongs() {
        showLoadingDialog();
        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> fetchedSongs) {
                songs = fetchedSongs;
                songAdapter.updateData(songs);
                loadPosts();
                progressDialog.dismiss();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                showToast("Error fetching songs: " + error);
            }
        });
    }

    private void loadPosts() {
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> fetchedPosts) {
                posts = fetchedPosts;
                songAdapter.updateDataPost(posts);
            }

            @Override
            public void onError(String error) {
                showToast("Error fetching posts: " + error);
            }
        });
    }

    private void searchSongsByTitle(String query) {
        songBUS.searchSongsByTitle(query, new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                songAdapter.updateData(songs);
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(LOADING_MESSAGE);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}