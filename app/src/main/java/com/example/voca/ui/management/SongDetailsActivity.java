package com.example.voca.ui.management;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voca.R;
import com.example.voca.bus.SongBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.SongDTO;
import com.example.voca.service.FileUploader;
import com.example.voca.service.LoadImage;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class SongDetailsActivity extends AppCompatActivity {
    private TextView textUploader, textCreatedAt, textRecordedPeople;
    private EditText editTitle, editMp3File, editYoutubeId;
    private ImageView imageThumbnail;
    private Button btnSave, btnDelete, btnPick, btnDeleteFile;
    private SongBUS songBUS;
    private String songId;
    private int recordedPeople;
    private String Mp3Path; // Đường dẫn MP3

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_song_details);

        songBUS = new SongBUS();

        editTitle = findViewById(R.id.editTitle);
        textUploader = findViewById(R.id.textUploader);
        textCreatedAt = findViewById(R.id.editCreatedAt);
        textRecordedPeople = findViewById(R.id.textRecordedPeople);
        editMp3File = findViewById(R.id.editMp3File);
        editYoutubeId = findViewById(R.id.editYoutubeId);
        imageThumbnail = findViewById(R.id.imageThumbnail);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnPick = findViewById(R.id.pick);
        btnDeleteFile = findViewById(R.id.delete_file_button);

        textCreatedAt.setEnabled(false);

        Intent intent = getIntent();
        songId = intent.getStringExtra("song_id");
        String songTitle = intent.getStringExtra("title");
        String uploadedBy = intent.getStringExtra("uploaded_by");
        String createdAt = intent.getStringExtra("created_at");
        String mp3File = intent.getStringExtra("mp3_file");
        String youtubeId = intent.getStringExtra("youtube_id");
        String thumbnailUrl = intent.getStringExtra("thumbnail");
        recordedPeople = intent.getIntExtra("recorded_people", 0);

        Mp3Path = mp3File;

        editTitle.setText(songTitle);
        textUploader.setText(uploadedBy);
        textCreatedAt.setText(createdAt);
        editMp3File.setText(mp3File);
        editYoutubeId.setText(youtubeId);
        textRecordedPeople.setText(String.valueOf(recordedPeople));

        new LoadImage(imageThumbnail).execute(thumbnailUrl);

        com.google.android.material.textfield.TextInputLayout youtubeIdLayout = findViewById(R.id.youtube_id_layout);

        editYoutubeId.addTextChangedListener(new TextWatcher() {
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
                }
            }
        });

        btnSave.setOnClickListener(v -> updateSong());
        btnDelete.setOnClickListener(v -> deleteSong());
        btnPick.setOnClickListener(v -> openFilePicker());
        btnDeleteFile.setOnClickListener(v -> deleteFile());
        setClickOnNavigationButton();
    }

    private void setClickOnNavigationButton() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void updateSong() {
        String newTitle = editTitle.getText().toString();
        String newMp3File = editMp3File.getText().toString();
        String newYoutubeId = editYoutubeId.getText().toString();

        if (newTitle.isEmpty()) {
            Toast.makeText(SongDetailsActivity.this, "Vui lòng điền Tiêu đề", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newMp3File.isEmpty()) {
            Toast.makeText(SongDetailsActivity.this, "Vui lòng điền Đường dẫn MP3", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newYoutubeId.isEmpty()) {
            Toast.makeText(SongDetailsActivity.this, "Vui lòng điền Youtube ID", Toast.LENGTH_SHORT).show();
            return;
        }

        songBUS.fetchSongById(songId, new SongBUS.OnSongFetchedListener() {
            @Override
            public void onSongFetched(SongDTO song) {
                SongDTO updatedSong = new SongDTO(
                        songId,
                        newYoutubeId,
                        newTitle,
                        newMp3File,
                        "http://img.youtube.com/vi/" + newYoutubeId + "/0.jpg",
                        null,
                        null,
                        song.getRecorded_people()
                );

                songBUS.updateSong(songId, updatedSong, new SongBUS.OnSongUpdatedListener() {
                    @Override
                    public void onSongUpdated(SongDTO song) {
                        Toast.makeText(SongDetailsActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(SongDetailsActivity.this, "Lỗi cập nhật: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SongDetailsActivity.this, "Lỗi khi lấy thông tin bài hát: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSong() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa bài hát này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    songBUS.deleteSong(songId, new SongBUS.OnSongDeletedListener() {
                        @Override
                        public void onSongDeleted() {
                            Toast.makeText(SongDetailsActivity.this, "Xóa thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(SongDetailsActivity.this, "Lỗi xóa: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, 3);
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
            new FileUploader().run(this, audioUri, new FileUploader.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String url) {
                    runOnUiThread(() -> {
                        Mp3Path = url;
                        editMp3File.setText(url);
                        Toast.makeText(SongDetailsActivity.this, "Tải lên nhạc nền thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onFailure() {
                    runOnUiThread(() -> {
                        Toast.makeText(SongDetailsActivity.this, "Tải lên nhạc nền thất bại", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void deleteFile() {
        if (Mp3Path != null) {
            new FileUploader().deleteFileByURL(Mp3Path);
            Mp3Path = "";
            editMp3File.setText("");
            Toast.makeText(this, "Đã xóa file trên cloud", Toast.LENGTH_SHORT).show();
        } else if (Mp3Path.isEmpty() && editMp3File.getText().length() > 0) {
            editMp3File.setText("");
            Toast.makeText(this, "Không có file nào được tải lên", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Không có file để xóa", Toast.LENGTH_SHORT).show();
        }
    }
}