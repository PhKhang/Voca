package com.example.voca.ui.management;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.voca.R;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.SongDTO;
import com.example.voca.service.LoadImage;

public class SongDetailsActivity extends AppCompatActivity {
    private TextView textUploader, textCreatedAt;
    private EditText editTitle, editMp3File, editYoutubeId;
    private ImageView imageThumbnail;
    private Button btnSave;
    private SongBUS songBUS;
    private String songId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_details);

        songBUS = new SongBUS();

        editTitle = findViewById(R.id.editTitle);
        textUploader = findViewById(R.id.textUploader);
        textCreatedAt = findViewById(R.id.editCreatedAt);
        editMp3File = findViewById(R.id.editMp3File);
        editYoutubeId = findViewById(R.id.editYoutubeId);
        imageThumbnail = findViewById(R.id.imageThumbnail);
        btnSave = findViewById(R.id.btnSave);

        textCreatedAt.setEnabled(false);

        Intent intent = getIntent();
        songId = intent.getStringExtra("song_id");
        String songTitle = intent.getStringExtra("title");
        String uploadedBy = intent.getStringExtra("uploaded_by");
        String createdAt = intent.getStringExtra("created_at");
        String mp3File = intent.getStringExtra("mp3_file");
        String youtubeId = intent.getStringExtra("youtube_id");
        String thumbnailUrl = intent.getStringExtra("thumbnail");

        editTitle.setText(songTitle);
        textUploader.setText(uploadedBy);
        textCreatedAt.setText(createdAt);
        editMp3File.setText(mp3File);
        editYoutubeId.setText(youtubeId);
        new LoadImage(imageThumbnail).execute(thumbnailUrl);

        btnSave.setOnClickListener(v -> updateSong());
    }

    private void updateSong() {
        String newTitle = editTitle.getText().toString();
        String newMp3File = editMp3File.getText().toString();
        String newYoutubeId = editYoutubeId.getText().toString();

        SongDTO updatedSong = new SongDTO(
                songId,
                newYoutubeId,
                newTitle,
                newMp3File,
                "http://img.youtube.com/vi/" + newYoutubeId + "/0.jpg", // Không cập nhật ảnh thumbnail
                null,
                textCreatedAt.getText().toString()
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
}
