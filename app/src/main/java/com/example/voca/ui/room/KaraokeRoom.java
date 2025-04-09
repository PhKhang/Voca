package com.example.voca.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.voca.R;
import com.example.voca.dao.RoomDAO;
import com.example.voca.dto.RoomDTO;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KaraokeRoom extends AppCompatActivity implements QueueFragment.OnGetAllSongInQueue {

    Boolean isPlaying = false;
    private String videoId = "WCXDr38Rq20";
    private YouTubePlayer youTubePlayerInstance;
    String roomId;
    RoomDTO currentRoom;
    ImageButton queue;
    ImageButton next;
    ImageButton prev;
    ImageButton mic;
    TextView host;
    TextView roomCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_karaoke_room);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        roomId = intent.getExtras().getString("roomId");

        ImageButton playButton = findViewById(R.id.play);
        queue = findViewById(R.id.queue);
        next = findViewById(R.id.next);
        prev = findViewById(R.id.prev);
        host = findViewById(R.id.host);
        roomCode = findViewById(R.id.roomCode);

        queue.setOnClickListener(v -> {
            QueueFragment queueFragment = new QueueFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, queueFragment);
            queueFragment.setOnGetAllSongInQueue(this);
            transaction.addToBackStack(null); // Optional, if you want back navigation
            transaction.commit();

            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        });

        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(youTubePlayerView);

        IFramePlayerOptions options = new IFramePlayerOptions.Builder()
                .controls(0)
                .rel(0)
                .ivLoadPolicy(0)
                .ccLoadPolicy(0)
                .autoplay(0)
                .build();

        youTubePlayerView.setEnableAutomaticInitialization(false);
        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.cueVideo(videoId, 0);
                youTubePlayerInstance = youTubePlayer;
//                youTubePlayer.mute();
                youTubePlayerInstance.addListener(new AbstractYouTubePlayerListener() {
                                                      @Override
                                                      public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                                                          super.onStateChange(youTubePlayer, state);
                                                          if (state == PlayerConstants.PlayerState.PAUSED || state == PlayerConstants.PlayerState.ENDED) {
                                                              playButton.setImageResource(R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24);
                                                              isPlaying = false;
                                                          } else if (state == PlayerConstants.PlayerState.PLAYING) {
                                                              playButton.setImageResource(R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24);
                                                              isPlaying = true;
                                                          }
                                                      }
                                                  }
                );
            }
        }, true, options);

        playButton.setOnClickListener(v -> {
            if (youTubePlayerInstance != null) {
                if (isPlaying)
                    youTubePlayerInstance.pause();
                else
                    youTubePlayerInstance.play();
            } else {
                Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            }
        });

        RoomDAO roomDAO = new RoomDAO();
        Toast.makeText(this, "Fetching room details: " + roomId, Toast.LENGTH_SHORT).show();
        roomDAO.getRoomByCode(roomId, new Callback<RoomDTO>() {
            @Override
            public void onResponse(Call<RoomDTO> call, Response<RoomDTO> response) {
                if (response.isSuccessful()) {
                    currentRoom = response.body();
                    Log.e("Room", "Room details: " + currentRoom.toString());
                    getSupportActionBar().setTitle(currentRoom.getName());
                    host.setText(currentRoom.getCreated_by().getUsername());
                    roomCode.setText("Mã phòng: " + currentRoom.getCode());
                } else {
                    Log.e("Error", "Failed to fetch room details: " + response.errorBody());
//                    Intent intent = new Intent(KaraokeRoom.this, CreateRoomActivity.class);
//                    intent.putExtra("message", "Room " + roomId + "not found");
//                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<RoomDTO> call, Throwable t) {
                Log.e("Error", "API call failed: " + t.getMessage());
                Intent intent = new Intent(KaraokeRoom.this, CreateRoomActivity.class);
                intent.putExtra("message", "Room " + roomId + "not found");
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onGetAllSongInQueue() {
        Log.e("", "Song list: " + currentRoom.getQueue().size());
    }
}