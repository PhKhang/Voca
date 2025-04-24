package com.example.voca.ui.management;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SongsManagementViewModel extends ViewModel {
    private final MutableLiveData<List<SongDTO>> songsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<PostDTO>> postsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final SongBUS songBUS = new SongBUS();
    private final PostBUS postBUS = new PostBUS();

    public LiveData<List<SongDTO>> getSongsLiveData() {
        return songsLiveData;
    }

    public LiveData<List<PostDTO>> getPostsLiveData() {
        return postsLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void fetchData() {
        AtomicInteger tasksCompleted = new AtomicInteger(0);
        int totalTasks = 2;

        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> fetchedSongs) {
                songsLiveData.setValue(fetchedSongs != null ? new ArrayList<>(fetchedSongs) : new ArrayList<>());
                if (tasksCompleted.incrementAndGet() == totalTasks) {
                }
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue("Error fetching songs: " + error);
            }
        });

        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> fetchedPosts) {
                postsLiveData.setValue(fetchedPosts != null ? new ArrayList<>(fetchedPosts) : new ArrayList<>());
                if (tasksCompleted.incrementAndGet() == totalTasks) {
                }
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue("Error fetching posts: " + error);
            }
        });
    }

    public void searchSongs(String query) {
        if (query.isEmpty()) {
            List<SongDTO> currentSongs = songsLiveData.getValue();
            songsLiveData.setValue(currentSongs != null ? new ArrayList<>(currentSongs) : new ArrayList<>());
        } else {
            songBUS.searchSongsByTitle(query, new SongBUS.OnSongsFetchedListener() {
                @Override
                public void onSongsFetched(List<SongDTO> fetchedSongs) {
                    songsLiveData.setValue(fetchedSongs != null ? new ArrayList<>(fetchedSongs) : new ArrayList<>());
                }

                @Override
                public void onError(String error) {
                    errorLiveData.setValue(error);
                }
            });
        }
    }
}