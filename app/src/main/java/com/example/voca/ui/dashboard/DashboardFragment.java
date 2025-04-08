package com.example.voca.ui.dashboard;

import static android.content.Context.MODE_PRIVATE;

import static androidx.core.content.ContextCompat.getColor;
import static com.google.android.material.internal.ViewUtils.hideKeyboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SearchView;
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
import com.example.voca.ui.ProfileViewActivity;
import com.google.android.exoplayer2.ExoPlayer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private SharedPostViewModel sharedPostViewModel;
    private PostAdapter postAdapter;
    RecyclerView recyclerView;
    private ExoPlayer player;
    private RecyclerView recyclerViewUsers;
    private UserBUS userBUS = new UserBUS();
    private UserAdapter userAdapter;
    private List<UserDTO> userList = new ArrayList<>(), filteredUsers = new ArrayList<>();


    public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        player = new ExoPlayer.Builder(requireContext()).build();

        recyclerView = root.findViewById(R.id.recyclerView_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(new ArrayList<>(), requireContext(), player);
        recyclerView.setAdapter(postAdapter);

        sharedPostViewModel = new ViewModelProvider(requireActivity()).get(SharedPostViewModel.class);

        String priorityPostId = null;
        if (getArguments() != null) {
            priorityPostId = getArguments().getString("priorityPostId");
        }
        sharedPostViewModel.fetchAllPosts(priorityPostId);

        sharedPostViewModel.getAllPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            Log.d("DashboardFragment", "LiveData updated, size: " + (posts != null ? posts.size() : 0));
            if (posts != null) {
                postAdapter.updateData(posts);
            }
        });

        recyclerViewUsers = root.findViewById(R.id.recyclerViewUsers);
        SearchView searchView = root.findViewById(R.id.searchView);

        userBUS.fetchUsers(new UserBUS.OnUsersFetchedListener() {
            @Override
            public void onUsersFetched(List<UserDTO> users) {
                userList = users;
                filteredUsers.addAll(userList);
            }

            @Override
            public void onError(String error) {
                Log.d("FetchUsersError", error);
            }
        });

        userAdapter = new UserAdapter(filteredUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);

        // Lắng nghe khi người dùng gõ vào ô tìm kiếm
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String query = newText.trim();

                if (!query.isEmpty()) {
                    searchUsers(query);
                } else {
                    hideSearchResults();
                }
                return true;
            }
        });

        // Xử lý focus (ẩn hiện RecyclerView)
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !searchView.getQuery().toString().isEmpty()) {
                recyclerViewUsers.setVisibility(View.VISIBLE);
            } else {
                recyclerViewUsers.setVisibility(View.GONE);
            }
        });

        return root;
    }

    private void searchUsers(String query) {
        if (userList == null) {
            return;
        }

        filteredUsers.clear();
        for (UserDTO user : userList) {
            if (user.getUsername().toLowerCase().contains(query.toLowerCase())) {
                filteredUsers.add(user);
            }
        }

        Collections.sort(filteredUsers, Comparator.comparing(UserDTO::getUsername));

        userAdapter.notifyDataSetChanged();
        recyclerViewUsers.setVisibility(View.VISIBLE);
    }

    private void hideSearchResults() {
        recyclerViewUsers.setVisibility(View.GONE);
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

    public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<UserDTO> userList;


        public UserAdapter(List<UserDTO> userList) {
            this.userList = userList;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_user_search, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserDTO user = userList.get(position);

            holder.txtUsername.setText(user.getUsername());

            Glide.with(requireContext())
                    .load(user.getAvatar())
                    .placeholder(R.drawable.default_account_avatar)
                    .into(holder.imgAvatar);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ProfileViewActivity.class);
                intent.putExtra("user_id", user.get_id());
                requireContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        public class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView txtUsername;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.img_avatar);
                txtUsername = itemView.findViewById(R.id.txt_username);
            }
        }
    }
}