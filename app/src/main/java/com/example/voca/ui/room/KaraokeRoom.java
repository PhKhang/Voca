package com.example.voca.ui.room;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import com.example.voca.R;
import com.example.voca.bus.RoomBUS;
import com.example.voca.dao.RoomDAO;
import com.example.voca.dto.RoomDTO;
import com.example.voca.dto.SongDTO;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KaraokeRoom extends AppCompatActivity implements SongUpdateCallback {

    Boolean isPlaying = false;
    String roomId;
    RoomDTO currentRoom;
    ImageButton queue;
    ImageButton next;
    ImageButton prev;
    ImageButton mic;
    TextView host;
    TextView roomCode;
    ImageButton btnCopyRoomCode;
    RoomBUS roomBUS;
    private String videoId = "WCXDr38Rq20";
    private YouTubePlayer youTubePlayerInstance;
    private YouTubePlayerTracker tracker;

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

        roomBUS = new RoomBUS();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("room");

        myRef.child(roomId).child("current").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentYouTubeId = snapshot.getValue(String.class);
                    Log.d("Room", "Current song ID: " + currentYouTubeId);
                    assert currentYouTubeId != null;
                    if (youTubePlayerInstance == null) {
                        return;
                    }
                    youTubePlayerInstance.loadVideo(currentYouTubeId, 0);
                    Log.d("Room", "Starting ID: " + currentYouTubeId);
                } else {
                    Log.d("Room", "No current song found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        ImageButton playButton = findViewById(R.id.play);
        queue = findViewById(R.id.queue);
        next = findViewById(R.id.next);
        next.setOnClickListener(v -> {
            if (youTubePlayerInstance == null) {
                return;
            }

            if (currentRoom.getQueue().isEmpty()) {
                Toast.makeText(this, "No songs in queue", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentSongId = currentRoom.getCurrent_song().getYoutube_id();
            int currentIndex = -1;
            for (int i = 0; i < currentRoom.getQueue().size(); i++) {
                if (currentRoom.getQueue().get(i).getYoutube_id().equals(currentSongId)) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex != -1 && currentIndex < currentRoom.getQueue().size() - 1) {
                currentRoom.setCurrent_song(currentRoom.getQueue().get(currentIndex + 1));
                myRef.child(roomId).child("current").setValue(currentRoom.getQueue().get(currentIndex + 1).getYoutube_id());
                Log.d("Room", "Next song ID: " + currentRoom.getQueue().get(currentIndex + 1).getYoutube_id());
//                youTubePlayerInstance.cueVideo(currentRoom.getCurrent_song().getYoutube_id(), 0);
//                youTubePlayerInstance.play();
            } else {
                Toast.makeText(this, "No next song in queue", Toast.LENGTH_SHORT).show();
            }
        });

        prev = findViewById(R.id.prev);
        prev.setOnClickListener(v -> {
            if (youTubePlayerInstance == null) {
                return;
            }

            if (currentRoom.getQueue().isEmpty()) {
                Toast.makeText(this, "No songs in queue", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentSongId = currentRoom.getCurrent_song().getYoutube_id();
            int currentIndex = -1;
            for (int i = 0; i < currentRoom.getQueue().size(); i++) {
                if (currentRoom.getQueue().get(i).getYoutube_id().equals(currentSongId)) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex > 0) {
                currentRoom.setCurrent_song(currentRoom.getQueue().get(currentIndex - 1));
                myRef.child(roomId).child("current").setValue(currentRoom.getQueue().get(currentIndex - 1).getYoutube_id());
                Log.d("Room", "Next song ID: " + currentRoom.getQueue().get(currentIndex - 1).getYoutube_id());
//                youTubePlayerInstance.cueVideo(currentRoom.getCurrent_song().getYoutube_id(), 0);
//                youTubePlayerInstance.play();
            } else {
                Toast.makeText(this, "No previous song in queue", Toast.LENGTH_SHORT).show();
            }
        });

        host = findViewById(R.id.host);
        roomCode = findViewById(R.id.roomCode);
        btnCopyRoomCode = findViewById(R.id.btnCopyRoomCode);

        btnCopyRoomCode.setOnClickListener(v -> {
            String fullText = roomCode.getText().toString();
            String textToCopy = fullText.replace("Mã phòng: ", "").trim();

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Room Code", textToCopy);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Room code copied!", Toast.LENGTH_SHORT).show();
            }
        });

        queue.setOnClickListener(v -> {
            KotlinFragment fragment = new KotlinFragment();

            fragment.setSongUpdateCallback(this);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null) // Optional, if you want back navigation
                    .commit();
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

        tracker = new YouTubePlayerTracker();
        youTubePlayerView.setEnableAutomaticInitialization(false);
        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
//                youTubePlayer.cueVideo(videoId, 0);
                youTubePlayerInstance = youTubePlayer;
//                youTubePlayer.mute();
                youTubePlayerInstance.addListener(tracker);
                youTubePlayerInstance.addListener(new AbstractYouTubePlayerListener() {
                                                      @Override
                                                      public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                                                          super.onStateChange(youTubePlayer, state);
                                                          YouTubePlayerTracker newTracker = new YouTubePlayerTracker();
                                                          youTubePlayer.addListener(newTracker);
//                                                          Log.d("Room", "Video duration on loaded" + newTracker.getVideoDuration());
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
            Log.d("Room", "Current time: " + tracker.getVideoDuration());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = null;
            try {
                date = sdf.parse(currentRoom.getCurrent_song_start_time());
            } catch (ParseException e) {
//                throw new RuntimeException(e);
                Log.e("Room", "Error parsing date: " + e.getMessage());
                date = new Date();
            }

            Date now = new Date();
            long diffInMillis = now.getTime() - date.getTime(); // difference in milliseconds
            long diffInSeconds = diffInMillis / 1000;

            if (youTubePlayerInstance != null) {
                if (currentRoom.getCurrent_song() == null) {
                    Toast.makeText(this, "No song is playing", Toast.LENGTH_SHORT).show();
//                    return;
                    if (currentRoom.getQueue().isEmpty()) {
                        Toast.makeText(this, "No songs in queue", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentRoom.setCurrent_song(currentRoom.getQueue().get(0));
                    if (diffInSeconds >= 24 * 60 * 60) {
                        currentRoom.setCurrent_song_start_time(String.valueOf(System.currentTimeMillis()));
                        youTubePlayerInstance.loadVideo(currentRoom.getCurrent_song().getYoutube_id(), 0);
                        roomBUS.updateRoom(currentRoom.get_id(), currentRoom, new RoomBUS.OnRoomUpdatedListener() {
                            @Override
                            public void onRoomUpdated(RoomDTO room) {
                                Toast.makeText(KaraokeRoom.this, "Update play time successfully", Toast.LENGTH_SHORT).show();
                                Log.d("Room", "Updated play time: ");
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(KaraokeRoom.this, "Error adding song: " + error, Toast.LENGTH_SHORT).show();
                                Log.e("Error", "Failed to add song: " + error);
                            }
                        });
                    }
                    else {
                        youTubePlayerInstance.loadVideo(currentRoom.getCurrent_song().getYoutube_id(), diffInSeconds);
                    }
                }
                if (isPlaying)
                    youTubePlayerInstance.pause();
                else
                    youTubePlayerInstance.play();
            } else {
                Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            }

//            Intent composeIntent = new Intent(KaraokeRoom.this, JetpackActivity.class);
//            startActivity(composeIntent);

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
    public List<SongDTO> getQueue() {
        return currentRoom.getQueue();
    }

    @Override
    public void addSong(SongDTO song) {
        Toast.makeText(this, "Before add: " + currentRoom.getQueue().size(), Toast.LENGTH_SHORT).show();
        currentRoom.getQueue().add(song);
        roomBUS.updateRoom(currentRoom.get_id(), currentRoom, new RoomBUS.OnRoomUpdatedListener() {
            @Override
            public void onRoomUpdated(RoomDTO room) {
                Toast.makeText(KaraokeRoom.this, "Song added successfully", Toast.LENGTH_SHORT).show();
                Log.d("Room", "Song added: " + song.getYoutube_id());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(KaraokeRoom.this, "Error adding song: " + error, Toast.LENGTH_SHORT).show();
                Log.e("Error", "Failed to add song: " + error);
            }
        });
        Toast.makeText(this, "After add: " + currentRoom.getQueue().size(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void removeSong(String songId) {
        for (int i = 0; i < currentRoom.getQueue().size(); i++) {
            if (currentRoom.getQueue().get(i).get_id().equals(songId)) {
                currentRoom.getQueue().remove(i);
                roomBUS.updateRoom(currentRoom.get_id(), currentRoom, new RoomBUS.OnRoomUpdatedListener() {
                    @Override
                    public void onRoomUpdated(RoomDTO room) {
                        Toast.makeText(KaraokeRoom.this, "Song removed successfully", Toast.LENGTH_SHORT).show();
                        Log.d("Room", "Song removed: " + songId);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(KaraokeRoom.this, "Error removing song: " + error, Toast.LENGTH_SHORT).show();
                        Log.e("Error", "Failed to remove song: " + error);
                    }
                });
                break;
            }
        }
    }

    @Override
    public void singSong(String songId) {
        currentRoom.setCurrent_song_start_time(String.valueOf(System.currentTimeMillis()));
        for (int i = 0; i < currentRoom.getQueue().size(); i++) {
            if (currentRoom.getQueue().get(i).get_id().equals(songId)) {
                Log.d("Song", "Singing song: " + currentRoom.getQueue().get(i).get_id());
                videoId = currentRoom.getQueue().get(i).getYoutube_id();
                youTubePlayerInstance.loadVideo(videoId, 0);
                break;
            }
        }
    }
}