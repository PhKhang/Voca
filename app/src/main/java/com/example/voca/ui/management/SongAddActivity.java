package com.example.voca.ui.management;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.SongBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.service.FetchVideoTitleTask;
import com.example.voca.service.FileUploader;
import com.example.voca.service.LoadImage;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SongAddActivity extends AppCompatActivity {
    private SongBUS songBus;
    private UserBUS userBus;
    private Uri fileUri;
    private String videoId = "";
    private String Mp3Path = "";
    private EditText mp3LinkEditText;
    private ImageView imageThumbnail;
    private EditText titleEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_songsmanagement);

        songBus = new SongBUS();
        userBus = new UserBUS();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        mp3LinkEditText = findViewById(R.id.mp3_link_edittext);
        EditText youtubeIdEditText = findViewById(R.id.youtube_id_edittext);
        titleEditText = findViewById(R.id.title_edittext);
        imageThumbnail = findViewById(R.id.imageThumbnail);

        com.google.android.material.textfield.TextInputLayout youtubeIdLayout = findViewById(R.id.youtube_id_layout);
        youtubeIdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (input.length() < 11) {
                    youtubeIdLayout.setError("ID không hợp lệ");
                } else {
                    youtubeIdLayout.setError(null);
                    new LoadImage(imageThumbnail).execute("http://img.youtube.com/vi/" + input + "/0.jpg");
                    new FetchVideoTitleTask(SongAddActivity.this, titleEditText).execute(input);
                }
            }
        });

        Button submitButton = findViewById(R.id.submit_button);

        Button pickButton = findViewById(R.id.pick);
        pickButton.setOnClickListener(view -> openFilePicker()); // Mở trình chọn file
        setClickOnNavigationButton();
        submitButton.setOnClickListener(v -> {
            String inputMp3Link = mp3LinkEditText.getText().toString().trim();
            String inputVideoId = youtubeIdEditText.getText().toString().trim();
            String inputTitleVideo = titleEditText.getText().toString().trim();

            if (inputTitleVideo.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền Tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }
            if (inputMp3Link.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền Đường dẫn MP3", Toast.LENGTH_SHORT).show();
                return;
            }
            if (inputVideoId.length() != 11) {
                Toast.makeText(this, "YouTube ID không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            Mp3Path = inputMp3Link;
            videoId = inputVideoId;
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
                                Toast.makeText(SongAddActivity.this, "Bài hát mới được tạo: " + song.getTitle() + " với ID: " + song.get_id(), Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(SongAddActivity.this, "Lỗi khi tạo bài hát", Toast.LENGTH_SHORT).show();
                                Log.e("SongModule", "Lỗi khi tạo bài hát: " + error);
                            }
                        });

                    } else {
                        Toast.makeText(SongAddActivity.this, "Lỗi khi tạo bài hát", Toast.LENGTH_SHORT).show();
                        Log.e("UserModule", "Không tìm thấy người dùng với ID: " + userId);
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(SongAddActivity.this, "Lỗi khi tạo bài hát", Toast.LENGTH_SHORT).show();
                    Log.e("UserModule", "Lỗi khi tìm người dùng: " + error);
                }
            });
        });

        Button deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            if (!Mp3Path.isEmpty()) {
                new FileUploader().deleteFileByURL(Mp3Path);

                Mp3Path = "";
                mp3LinkEditText.setText("");
                Toast.makeText(this, "Đã xóa file trên cloud", Toast.LENGTH_SHORT).show();
            } else if (Mp3Path.isEmpty() && (mp3LinkEditText.getText().length() > 0)) {
                mp3LinkEditText.setText("");
                Toast.makeText(this, "Không có file nào được tải lên", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setClickOnNavigationButton() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
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
        if (requestCode == 3 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri audioUri = data.getData();
                showConfirmAudioDialog(audioUri);
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
            Toast.makeText(SongAddActivity.this, "Đang tải lên", Toast.LENGTH_SHORT).show();
            new FileUploader().run(this, audioUri, new FileUploader.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String url) {
                    runOnUiThread(() -> {
                        Mp3Path = url;
                        mp3LinkEditText.setText(url);
                        Toast.makeText(SongAddActivity.this, "Tải lên thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onFailure() {
                    runOnUiThread(() -> {
                        Log.e("UploadAudioFailed", "Tải lên thất bại!");
                        Toast.makeText(SongAddActivity.this, "Tải lên thất bại!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
}
