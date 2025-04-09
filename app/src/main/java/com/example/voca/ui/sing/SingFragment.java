package com.example.voca.ui.sing;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
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
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sing, container, false);

        context = getContext();

        tabLayout = view.findViewById(R.id.tabLayout);
        listView = view.findViewById(R.id.listViewSing);

        songList = new ArrayList<>();
        postList = new ArrayList<>();
        songBUS = new SongBUS();
        postBUS = new PostBUS();

        searchView = view.findViewById(R.id.searchView);
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

        fetchSongs(0);

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
                filterBySearchAndTab(newText);
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

        singAdapter = new SingAdapter(context, postList, finalFilteredList);
        listView.setAdapter(singAdapter);
    }

    private void resetAndFetchData(int tabPosition) {
        if (songList != null) songList.clear();
        if (postList != null) postList.clear();
        if (singAdapter != null) singAdapter.notifyDataSetChanged();

        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }

        fetchSongs(tabPosition);
    }

    private void fetchSongs(int tabPosition) {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang tải bài hát...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                songList = songs; // Lưu danh sách bài hát gốc
                fetchPosts(tabPosition); // Truyền tabPosition vào fetchPosts
                // Không gọi filterSongsByTab ở đây nữa
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Error fetching songs: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPosts(int tabPosition) {
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                postList = posts;
                // Sau khi có cả songList và postList, giờ mới lọc và cập nhật UI
                songList = filterSongsByTab(songList, tabPosition);
                if (context != null) {
                    singAdapter = new SingAdapter(context, postList, songList);
                    listView.setAdapter(singAdapter);
                }
                progressDialog.dismiss();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Error fetching posts: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<SongDTO> filterSongsByTab(List<SongDTO> songs, int tabPosition) {
        List<SongDTO> filteredList = new ArrayList<>(songs); // Sao chép danh sách gốc để không thay đổi nó

        switch (tabPosition) {
            case 0: // Tab "Tất cả"
                // Không cần lọc, trả về toàn bộ danh sách
                return filteredList;

            case 1: // Tab "Nhiều lượt thích nhất"
                // Sắp xếp theo số lượt thích (tính từ posts)
                Collections.sort(filteredList, new Comparator<SongDTO>() {
                    @Override
                    public int compare(SongDTO song1, SongDTO song2) {
                        int likes1 = calculateTotalLikes(song1);
                        int likes2 = calculateTotalLikes(song2);
                        return Integer.compare(likes2, likes1); // Sắp xếp giảm dần
                    }
                });
                return filteredList;

            case 2: // Tab "Hát nhiều nhất"
                // Sắp xếp theo số người đã ghi âm (recorded_people)
                Collections.sort(filteredList, new Comparator<SongDTO>() {
                    @Override
                    public int compare(SongDTO song1, SongDTO song2) {
                        int recorded1 = song1.getRecorded_people();
                        int recorded2 = song2.getRecorded_people();
                        return Integer.compare(recorded2, recorded1); // Sắp xếp giảm dần
                    }
                });
                return filteredList;

            default:
                return filteredList; // Trường hợp không xác định, trả về danh sách gốc
        }
    }

    // Phương thức phụ để tính tổng số lượt thích cho một bài hát
    private int calculateTotalLikes(SongDTO song) {
        int totalLikes = 0;
        if (postList != null && song.get_id() != null) {
            for (PostDTO post : postList) {
                if (post != null && post.getSong_id() != null && post.getSong_id().get_id() != null) {
                    if (post.getSong_id().get_id().equals(song.get_id())) {
                        totalLikes += post.getLikes();
                    }
                }
            }
        }
        return totalLikes;
    }
}