package com.example.voca.ui.dashboard;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voca.R;
import com.example.voca.databinding.FragmentDashboardBinding;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    private ExoPlayer player;
    private PostAdapter.PostViewHolder currentViewHolder = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        player = new ExoPlayer.Builder(requireContext()).build();

//        final TextView textView = binding.textHomeTitle;
//        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            RecyclerView recyclerView = view.findViewById(R.id.recyclerView_posts);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            List<Post> postList = new ArrayList<>();
            postList.add(new Post("Người dùng 1", "1 phút trước", "post 1", R.drawable.ava, R.raw.examplevid));
            postList.add(new Post("Người dùng 2", "5 phút trước", "post 2", R.drawable.ava, R.raw.examplevid));
            postList.add(new Post("Người dùng 2", "5 phút trước", "post 2", R.drawable.ava, R.raw.examplevid));
            postList.add(new Post("Người dùng 2", "5 phút trước", "post 2", R.drawable.ava, R.raw.examplevid));

            PostAdapter adapter = new PostAdapter(postList);
            recyclerView.setAdapter(adapter);
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
    public class Post {
        private String username, postTime, content;
        private int avatarResId, videoResId;

        public Post(String username, String postTime, String content, int avatarResId, int videoResId) {
            this.username = username;
            this.postTime = postTime;
            this.content = content;
            this.avatarResId = avatarResId;
            this.videoResId = videoResId;
        }

        public String getUsername() { return username; }
        public String getPostTime() { return postTime; }
        public String getContent() { return content; }
        public int getAvatarResId() { return avatarResId; }
        public int getVideoResId() { return videoResId; }
    }

    public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        private List<Post> postList;
        public PostAdapter(List<Post> postList) {
            this.postList = postList;
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Post post = postList.get(position);
            holder.username.setText(post.getUsername());
            holder.postTime.setText(post.getPostTime());
            holder.postContent.setText(post.getContent());
            holder.userAvatar.setImageResource(post.getAvatarResId());

            if (post.getVideoResId() != 0) {
                holder.playerView.setVisibility(View.GONE);
                holder.playButton.setVisibility(View.VISIBLE);

                holder.playButton.setOnClickListener(v -> {
                    if (currentViewHolder != null && currentViewHolder != holder) {
                        stopVideo(currentViewHolder);
                    }
                    currentViewHolder = holder;

                    // Gán video mới
                    String videoPath = "android.resource://" + holder.itemView.getContext().getPackageName() + "/" + post.getVideoResId();
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(videoPath)));
                    player.prepare();
                    player.play();

                    // Cập nhật UI
                    holder.playButton.setVisibility(View.GONE);
                    holder.playerView.setVisibility(View.VISIBLE);
                    holder.playerView.setPlayer(player);

                });


                // Khi video chạy hết, reset lại trạng thái ban đầu
                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_ENDED) {
                            holder.playerView.setVisibility(View.GONE);
                            holder.playButton.setVisibility(View.VISIBLE);
                            player.seekTo(0);
                            player.pause();
                        }
                    }
                });

            } else {
                holder.playerView.setVisibility(View.GONE);
            }
        }
        private void stopVideo(PostViewHolder holder) {
            player.stop();
            holder.playButton.setVisibility(View.VISIBLE);
            holder.playerView.setVisibility(View.GONE);
        }
        @Override
        public int getItemCount() {
            return postList.size();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            TextView username, postTime, postContent;
            ImageView userAvatar;
            PlayerView playerView;
            FrameLayout videoContainer;
            ImageButton playButton;

            public PostViewHolder(@NonNull View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.txt_username);
                postTime = itemView.findViewById(R.id.txt_post_time);
                postContent = itemView.findViewById(R.id.txt_post_content);
                userAvatar = itemView.findViewById(R.id.avatarImage);

                playerView = itemView.findViewById(R.id.player_view);
                playButton = itemView.findViewById(R.id.btn_play_video);
                videoContainer = itemView.findViewById(R.id.video_container);
            }
        }
    }

}