package com.example.voca.ui.sing;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.ui.AdminActivity;
import com.example.voca.ui.management.SongAdapter;

import java.util.ArrayList;
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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sing, container, false);

        context = getContext();

        listView = view.findViewById(R.id.listViewSing);
        songList = new ArrayList<>();
        postList = new ArrayList<>();
        songBUS = new SongBUS();
        postBUS = new PostBUS();

        fetchSongs();

        return view;
    }

    private void fetchSongs() {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Loading songs...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                songList = songs;
                fetchPosts();
                progressDialog.dismiss();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Error fetching songs: " + error, Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void fetchPosts() {
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                postList = posts;
                if (context != null)
                    singAdapter = new SingAdapter(context, postList, songList);
                listView.setAdapter(singAdapter);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error fetching posts: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

