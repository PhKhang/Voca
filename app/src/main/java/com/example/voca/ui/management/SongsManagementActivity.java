package com.example.voca.ui.management;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.voca.R;
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
    private ExtendedFloatingActionButton addSongButton;
    private SearchView searchView;
    private ProgressDialog progressDialog;
    private SongsManagementViewModel viewModel;
    private Handler searchHandler;
    private Runnable searchRunnable;

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
        setupViewModel();
        setClickOnNavigationButton();
    }

    private void setClickOnNavigationButton() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeViews() {
        songListView = findViewById(R.id.listViewSongs);
        addSongButton = findViewById(R.id.addSong_button);
        searchView = findViewById(R.id.searchView);
    }

    private void initializeData() {
        songs = new ArrayList<>();
        posts = new ArrayList<>();
        songAdapter = new SongAdapter(this, songs, posts);
        songListView.setAdapter(songAdapter);
        searchHandler = new Handler(Looper.getMainLooper());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(SongsManagementViewModel.class);
        viewModel.getSongsLiveData().observe(this, fetchedSongs -> {
            songs = fetchedSongs != null ? new ArrayList<>(fetchedSongs) : new ArrayList<>();
            songAdapter.updateData(songs);
            checkDataLoaded();
        });
        viewModel.getPostsLiveData().observe(this, fetchedPosts -> {
            posts = fetchedPosts != null ? new ArrayList<>(fetchedPosts) : new ArrayList<>();
            songAdapter.updateDataPost(posts);
            checkDataLoaded();
        });
        viewModel.getErrorLiveData().observe(this, error -> {
            progressDialog.dismiss();
            showToast(error);
        });
        showLoadingDialog();
        viewModel.fetchData();
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
                viewModel.searchSongs(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> viewModel.searchSongs(newText);
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });
    }

    private void setupRootViewClickListener() {
        findViewById(R.id.root_layout).setOnClickListener(v -> {
            searchView.clearFocus();
            searchView.setQuery("", false);
            viewModel.searchSongs("");
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
        viewModel.fetchData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchView.setQuery("", false);
        findViewById(R.id.root_layout).requestFocus();
    }

    private void checkDataLoaded() {
        if (!songs.isEmpty() && !posts.isEmpty()) {
            progressDialog.dismiss();
        }
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(LOADING_MESSAGE);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}