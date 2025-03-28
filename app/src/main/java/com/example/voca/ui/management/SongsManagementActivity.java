package com.example.voca.ui.management;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.SongBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.service.FileUploader;

public class SongsManagementActivity extends AppCompatActivity {
    private SongBUS songBus;
    private UserBUS userBus;
    private Uri fileUri;
    private String videoId = "";
    private String Mp3Path = "";
    private EditText mp3LinkEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_songsmanagement);

        songBus = new SongBUS();
        userBus = new UserBUS();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        mp3LinkEditText = findViewById(R.id.mp3_link_edittext);
        EditText youtubeIdEditText = findViewById(R.id.youtube_id_edittext);
        EditText titleEditText = findViewById(R.id.title_edittext);

        Button submitButton = findViewById(R.id.submit_button);
        Button returnButton = findViewById(R.id.return_button);
        returnButton.setOnClickListener(v -> finish());

        Button pickButton = findViewById(R.id.pick);
        pickButton.setOnClickListener(view -> openFilePicker()); // Mở trình chọn file

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
                                thumbnailUrl, user, null, 0);

                        songBus.createSong(newSong, new SongBUS.OnSongCreatedListener() {
                            @Override
                            public void onSongCreated(SongDTO song) {
                                Toast.makeText(SongsManagementActivity.this, "Bài hát mới được tạo: " + song.getTitle() + " với ID: " + song.get_id(), Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(SongsManagementActivity.this, "Lỗi khi tạo bài hát", Toast.LENGTH_SHORT).show();
                                Log.e("SongModule", "Lỗi khi tạo bài hát: " + error);
                            }
                        });

                    } else {
                        Toast.makeText(SongsManagementActivity.this, "Lỗi khi tạo bài hát", Toast.LENGTH_SHORT).show();
                        Log.e("UserModule", "Không tìm thấy người dùng với ID: " + userId);
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(SongsManagementActivity.this, "Lỗi khi tạo bài hát", Toast.LENGTH_SHORT).show();
                    Log.e("UserModule", "Lỗi khi tìm người dùng: " + error);
                }
            });
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, 3); // Request code là 3
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri audioUri = data.getData(); // Lấy URI của file âm thanh
                showConfirmAudioDialog(audioUri); // Hiển thị Dialog xác nhận
            }
        }
    }

    private void showConfirmAudioDialog(Uri audioUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_confirm_audio, null);
        builder.setView(dialogView);

        TextView audioName = dialogView.findViewById(R.id.audio_name);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        audioName.setText(audioUri.getLastPathSegment());

        AlertDialog dialog = builder.create();
        dialog.show();

        btnConfirm.setOnClickListener(v -> {
            new FileUploader().run(this, audioUri, new FileUploader.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String url) {
                    runOnUiThread(() -> {
                        Mp3Path = url;
                        mp3LinkEditText.setText(url);
                        Toast.makeText(SongsManagementActivity.this, "Tải lên thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onFailure() {
                    Log.d("UploadAudioFailed", "Tải lên thất bại!");
                    dialog.dismiss();
                    Toast.makeText(SongsManagementActivity.this, "Tải lên thất bại!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
}
