package com.example.voca.ui.room;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.voca.R;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class KaraokeRoom extends AppCompatActivity {

    Boolean isPlaying = false;
    private String videoId = "WCXDr38Rq20";
    private YouTubePlayer youTubePlayerInstance;

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

        getSupportActionBar().setTitle("Karaoke Room");

        ImageButton playButton = findViewById(R.id.play);

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
    }
}