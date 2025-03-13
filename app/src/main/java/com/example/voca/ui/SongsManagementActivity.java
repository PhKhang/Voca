package com.example.voca.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.example.voca.R;
import com.example.voca.bus.SongBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;

public class SongsManagementActivity extends Activity {
    private SongBUS songBus;
    private UserBUS userBus;

    String videoId = "";
    String Mp3Path = "https://pub-9baa3a81ecf34466aeb5591929ebf0b3.r2.dev/youtube_LoKtEI9RONw_audio.mp3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songsmanagement);

        songBus = new SongBUS();
        userBus = new UserBUS();

        String userId = "67c9a9733177a378bbc0d7a7";

        EditText mp3LinkEditText = findViewById(R.id.mp3_link_edittext);
        EditText youtubeIdEditText = findViewById(R.id.youtube_id_edittext);
        EditText titleEditText = findViewById(R.id.title_edittext);

        Button submitButton = findViewById(R.id.submit_button);
        Button returnButton = findViewById(R.id.return_button);
        returnButton.setOnClickListener(v -> finish());

        submitButton.setOnClickListener(v -> {
            String inputMp3Link = mp3LinkEditText.getText().toString().trim();
            String inputVideoId = youtubeIdEditText.getText().toString().trim();
            String inputTitleVideo = titleEditText.getText().toString().trim();
            if (!inputMp3Link.isEmpty()) {
                Mp3Path = inputMp3Link;
            }

            if (!inputVideoId.isEmpty()) {
                videoId = inputVideoId;
            }
            String thumbnailUrl = "http://img.youtube.com/vi/" + videoId + "/0.jpg";

            userBus.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
                @Override
                public void onUserFetched(UserDTO user) {
                    if (user != null) {
                        Log.d("UserModule", "Tìm thấy người dùng: " + user.getUsername());

                        SongDTO newSong = new SongDTO(null, videoId, inputTitleVideo, Mp3Path,
                                thumbnailUrl, user, null);

                        songBus.createSong(newSong, new SongBUS.OnSongCreatedListener() {
                            @Override
                            public void onSongCreated(SongDTO song) {
                                Log.d("SongModule", "Bài hát mới được tạo: " + song.getTitle() + " với ID: " + song.get_id());
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("SongModule", "Lỗi khi tạo bài hát: " + error);
                            }
                        });

                    } else {
                        Log.e("UserModule", "Không tìm thấy người dùng với ID: " + userId);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("UserModule", "Lỗi khi tìm người dùng: " + error);
                }
            });
        });


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
