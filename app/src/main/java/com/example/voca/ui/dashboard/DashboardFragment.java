package com.example.voca.ui.dashboard;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voca.R;
import com.example.voca.bus.LikeBUS;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.databinding.FragmentDashboardBinding;
import com.example.voca.dto.LikeDTO;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.ui.PostAdapter;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private PostAdapter postAdapter;
    RecyclerView recyclerView;
    private ExoPlayer player;

    public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        player = new ExoPlayer.Builder(requireContext()).build();

        recyclerView = root.findViewById(R.id.recyclerView_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(new ArrayList<>(), requireContext(), player);
        recyclerView.setAdapter(postAdapter);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        dashboardViewModel.getPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            Log.d("DashboardFragment", "LiveData updated, size: " + (posts != null ? posts.size() : 0));
            postAdapter.updateData(posts);

        });
        // Xem tổng số posts
//        Toast.makeText(requireContext(), Integer.toString(postAdapter.getItemCount()), Toast.LENGTH_SHORT).show();

        dashboardViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            Log.d("DashboardFragment error", error);
        });
//        Test lớp SharedPreferences


//        Log.d("UserFirebaseUid", firebaseUid);

        return root;
    }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);



        }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
            player = null;
        }

        binding = null;
    }

    public static class TimeFormatter {
        public static String formatTime(String mongoTime) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date postDate = sdf.parse(mongoTime);
                Date now = new Date(); // Lấy thời gian hiện tại

                long diff = now.getTime() - postDate.getTime();
                long seconds = diff / 1000;

                if (seconds < 60) return "vừa xong";
                if (seconds < 3600) return (seconds / 60) + " phút";
                if (seconds < 86400) return (seconds / 3600) + " giờ";

                return (seconds / 86400) + " ngày";
            } catch (ParseException e) {
                return "Không xác định";
            }
        }
    }


}