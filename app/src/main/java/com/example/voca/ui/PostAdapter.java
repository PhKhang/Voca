package com.example.voca.ui;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voca.R;
import com.example.voca.bus.LikeBUS;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.LikeDTO;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.service.LoadImage;
import com.example.voca.ui.dashboard.DashboardFragment;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<PostDTO> postList;
    private Context context;
    ExoPlayer player;

    private LikeBUS likeBUS = new LikeBUS();
    private UserBUS userBUS = new UserBUS();
    private PostBUS postBUS = new PostBUS();

    PostViewHolder currentViewHolder;

    public PostAdapter(List<PostDTO> postList, Context context, ExoPlayer player) {
        this.postList = postList;
        this.context = context;
        this.player = player;
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
        holder.postTime.setText(DashboardFragment.TimeFormatter.formatTime(post.getCreated_at()));
        holder.postContent.setText(post.getCaption());
        Glide.with(context)
                .load(post.getUser_id().getAvatar())
                .placeholder(R.drawable.ava) // Ảnh mặc định nếu tải chậm
                .error(R.drawable.ava) // Ảnh nếu lỗi tải
                .into(holder.userAvatar);

        if (post.getSong_id() != null) {
            Glide.with(context)
                    .load(post.getSong_id().getThumbnail())
                    .placeholder(R.drawable.default_thumbnail) // Ảnh mặc định nếu tải chậm
                    .error(R.drawable.ava) // Ảnh nếu lỗi tải
                    .into(holder.songThumbnail);
            holder.songName.setText(post.getSong_id().getTitle());
        }

        if (context instanceof MainActivity){
            holder.userAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileViewActivity.class);
                intent.putExtra("user_id", post.getUser_id().get_id());
                context.startActivity(intent);
            });

        }

        holder.likeNumber.setText(Integer.toString(post.getLikes()));

        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", MODE_PRIVATE);
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

        ImageView songThumbnail;
        TextView songName;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.txt_username);
            postTime = itemView.findViewById(R.id.txt_post_time);
            postContent = itemView.findViewById(R.id.txt_post_content);
            userAvatar = itemView.findViewById(R.id.avatarImage);

            songThumbnail = itemView.findViewById(R.id.songThumbnail);
            songName = itemView.findViewById(R.id.songName);
            likeBtn = itemView.findViewById(R.id.btn_like);
            likeNumber = itemView.findViewById(R.id.txt_like_count);

            playerView = itemView.findViewById(R.id.player_view);
            playButton = itemView.findViewById(R.id.btn_play_video);
            videoContainer = itemView.findViewById(R.id.video_container);
        }
    }
}
