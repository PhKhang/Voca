package com.example.voca.ui.sing;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.ui.adapter.SingAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SingFragment extends Fragment {
    private ListView listView;
    private List<SongDTO> songList;
    private List<PostDTO> postList;
    private SingAdapter singAdapter;
    private SongBUS songBUS;
    private PostBUS postBUS;
    private ProgressDialog progressDialog;
    private Context context;
    private TabLayout tabLayout;
    private SearchView searchView;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private Map<String, Integer> songLikesCache;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sing, container, false);

        context = getContext();

        tabLayout = view.findViewById(R.id.tabLayout);
        listView = view.findViewById(R.id.listViewSing);

        songList = new ArrayList<>();
        postList = new ArrayList<>();
        songLikesCache = new HashMap<>();
        songBUS = new SongBUS();
        postBUS = new PostBUS();

        singAdapter = new SingAdapter(context, postList, songList);
        listView.setAdapter(singAdapter);

        searchView = view.findViewById(R.id.searchView);
        searchHandler = new Handler(Looper.getMainLooper());
        setupSearchViewListener();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                resetAndFetchData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                resetAndFetchData(tab.getPosition());
            }
        });

        fetchData(0);

        return view;
    }

    private void setupSearchViewListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBySearchAndTab(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterBySearchAndTab(newText);
                searchHandler.postDelayed(searchRunnable, 300); // 300ms debounce
                return true;
            }
        });
    }

    private void filterBySearchAndTab(String query) {
        if (songList == null) return;

        int currentTab = tabLayout.getSelectedTabPosition();
        List<SongDTO> filteredByTab = filterSongsByTab(songList, currentTab);

        List<SongDTO> finalFilteredList = new ArrayList<>();
        for (SongDTO song : filteredByTab) {
            if (song.getTitle().toLowerCase().contains(query.toLowerCase())) {
                finalFilteredList.add(song);
            }
        }

        singAdapter.updateData(postList, finalFilteredList);
        singAdapter.notifyDataSetChanged();
    }

    private void resetAndFetchData(int tabPosition) {
        songList.clear();
        postList.clear();
        songLikesCache.clear();
        singAdapter.updateData(postList, songList);
        singAdapter.notifyDataSetChanged();

        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }

        fetchData(tabPosition);
    }

    private void fetchData(int tabPosition) {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        AtomicInteger tasksCompleted = new AtomicInteger(0);
        int totalTasks = 2;

        // Fetch songs
        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                songList = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
                if (tasksCompleted.incrementAndGet() == totalTasks) {
                    updateUI(tabPosition);
                }
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Error fetching songs: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch posts
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                postList = posts != null ? new ArrayList<>(posts) : new ArrayList<>();
                // Precompute total likes
                precomputeSongLikes();
                if (tasksCompleted.incrementAndGet() == totalTasks) {
                    updateUI(tabPosition);
                }
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Error fetching posts: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void precomputeSongLikes() {
        songLikesCache.clear();
        for (PostDTO post : postList) {
            if (post != null && post.getSong_id() != null && post.getSong_id().get_id() != null) {
                String songId = post.getSong_id().get_id();
                songLikesCache.put(songId, songLikesCache.getOrDefault(songId, 0) + post.getLikes());
            }
        }
    }

    private void updateUI(int tabPosition) {
        List<SongDTO> filteredSongs = filterSongsByTab(songList, tabPosition);
        singAdapter.updateData(postList, filteredSongs);
        singAdapter.notifyDataSetChanged();
        progressDialog.dismiss();
    }

    private List<SongDTO> filterSongsByTab(List<SongDTO> songs, int tabPosition) {
        List<SongDTO> filteredList = new ArrayList<>(songs);

        switch (tabPosition) {
            case 0:
                return filteredList;

            case 1:
                Collections.sort(filteredList, (song1, song2) -> {
                    int likes1 = songLikesCache.getOrDefault(song1.get_id(), 0);
                    int likes2 = songLikesCache.getOrDefault(song2.get_id(), 0);
                    return Integer.compare(likes2, likes1);
                });
                return filteredList;

            case 2:
                Collections.sort(filteredList, (song1, song2) -> {
                    int recorded1 = song1.getRecorded_people();
                    int recorded2 = song2.getRecorded_people();
                    return Integer.compare(recorded2, recorded1);
                });
                return filteredList;

            default:
                return filteredList;
        }
    }
}