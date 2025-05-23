package com.example.voca.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeViewModel extends ViewModel {
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
            public void onSongsFetched(List<SongDTO> songs) {
                List<SongDTO> songList = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
                if (!songList.isEmpty()) {
                    Collections.sort(songList, (s1, s2) -> Integer.compare(s2.getRecorded_people(), s1.getRecorded_people()));
                    if (songList.size() > 3) {
                        songList = new ArrayList<>(songList.subList(0, 3));
                    }
                }
                songsLiveData.setValue(songList);
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
            public void onPostsFetched(List<PostDTO> posts) {
                List<PostDTO> postList = posts != null ? new ArrayList<>(posts) : new ArrayList<>();
                if (!postList.isEmpty()) {
                    Collections.sort(postList, (p1, p2) -> Integer.compare(p2.getLikes(), p1.getLikes()));
                    if (postList.size() > 3) {
                        postList = new ArrayList<>(postList.subList(0, 3));
                    }
                }
                postsLiveData.setValue(postList);
                if (tasksCompleted.incrementAndGet() == totalTasks) {

                }
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue("Error fetching posts: " + error);
            }
        });
    }
}