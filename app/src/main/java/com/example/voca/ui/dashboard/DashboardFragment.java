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
    private PostAdapter.PostViewHolder currentViewHolder = null;

    private LikeBUS likeBUS = new LikeBUS();
    private UserBUS userBUS = new UserBUS();
    private PostBUS postBUS = new PostBUS();

    public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        player = new ExoPlayer.Builder(requireContext()).build();

        recyclerView = root.findViewById(R.id.recyclerView_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(new ArrayList<>());
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

    public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        private List<PostDTO> postList;

        public PostAdapter(List<PostDTO> postList) {
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
            PostDTO post = postList.get(position);


            holder.username.setText(post.getUser_id().getUsername());
            holder.postTime.setText(TimeFormatter.formatTime(post.getCreated_at()));
            holder.postContent.setText(post.getCaption());

            Glide.with(requireContext())
                    .load(post.getUser_id().getAvatar())
                    .placeholder(R.drawable.ava) // Ảnh mặc định nếu tải chậm
                    .error(R.drawable.ava) // Ảnh nếu lỗi tải
                    .into(holder.userAvatar);
            holder.likeNumber.setText(Integer.toString(post.getLikes()));

            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = prefs.getString("userId", null);


            holder.likeBtn.setOnClickListener(v -> {
                //Toast.makeText(getContext(), "Likebtnpressed", Toast.LENGTH_SHORT).show();
                if (holder.likeBtn.getTag() != null && holder.likeBtn.getTag().equals("liked")){
                    likeBUS.checkLike(post.get_id(), userId, (isLiked, likeId) -> {
                        likeBUS.deleteLike(likeId, new LikeBUS.OnLikeDeletedListener() {
                            @Override
                            public void onLikeDeleted() {
                                holder.likeBtn.setImageResource(R.drawable.heart1);
                                holder.likeBtn.setTag("unliked");
                                post.setLikes(post.getLikes() - 1);
                                postBUS.updatePost(post.get_id(), post, new PostBUS.OnPostUpdatedListener(){

                                    @Override
                                    public void onPostUpdated(PostDTO post) {

                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.d("PostUpdateFailed", error);
                                    }
                                });
                            }
                            @Override
                            public void onError(String error) {
                                Log.d("LikePostError", error);
                            }
                        });
                    });
                } else {
                    userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
                        @Override
                        public void onUserFetched(UserDTO user) {
                            LikeDTO newLike = new LikeDTO(null, post, user, null);
                            likeBUS.createLike(newLike, new LikeBUS.OnLikeCreatedListener() {
                                @Override
                                public void onLikeCreated(LikeDTO like) {
                                    holder.likeBtn.setImageResource(R.drawable.heart3);
                                    holder.likeBtn.setTag("liked");

                                    post.setLikes(post.getLikes() + 1);
                                    postBUS.updatePost(post.get_id(), post, new PostBUS.OnPostUpdatedListener(){

                                        @Override
                                        public void onPostUpdated(PostDTO post) {

                                        }

                                        @Override
                                        public void onError(String error) {
                                            Log.d("PostUpdateFailed", error);
                                        }
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    Log.d("LikePostError", error);
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {

                        }
                    });
                }
            });

            likeBUS.checkLike(post.get_id(), userId, (isLiked, likeId) -> {
                if (isLiked) {
                    holder.likeBtn.setImageResource(R.drawable.heart3);
                    holder.likeBtn.setTag("liked");
                } else {
                    holder.likeBtn.setImageResource(R.drawable.heart1);
                    holder.likeBtn.setTag("unliked");
                }
            });

            if (!post.getAudio_url().equals("0")) {
                holder.playerView.setVisibility(View.GONE);
                holder.playButton.setVisibility(View.VISIBLE);

                holder.playButton.setOnClickListener(v -> {
                    if (currentViewHolder != null && currentViewHolder != holder) {
                        stopVideo(currentViewHolder);
                    }
                    currentViewHolder = holder;

//                    Toast.makeText(requireContext(), post.getAudio_url(), Toast.LENGTH_SHORT).show();
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(post.getAudio_url())));
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



        public void updateData(List<PostDTO> newPosts) {
            postList.clear();
            postList.addAll(newPosts);
            notifyDataSetChanged();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            TextView username, postTime, postContent, likeNumber;
            ImageView userAvatar;
            PlayerView playerView;
            FrameLayout videoContainer;
            ImageButton playButton;
            ImageButton likeBtn;

            public PostViewHolder(@NonNull View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.txt_username);
                postTime = itemView.findViewById(R.id.txt_post_time);
                postContent = itemView.findViewById(R.id.txt_post_content);
                userAvatar = itemView.findViewById(R.id.avatarImage);
                likeBtn = itemView.findViewById(R.id.btn_like);
                likeNumber = itemView.findViewById(R.id.txt_like_count);

                playerView = itemView.findViewById(R.id.player_view);
                playButton = itemView.findViewById(R.id.btn_play_video);
                videoContainer = itemView.findViewById(R.id.video_container);
            }
        }
    }


}