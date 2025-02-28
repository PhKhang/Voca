package com.example.voca.ui.record;
import android.content.ContentValues;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.voca.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RecordResultActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private SeekBar seekBarTime, seekBarVolume;
    private TextView tvCurrentTime;
    private Button btnPlayPause;
    private AudioManager audioManager;
    private Button saveButton;

    private String combinedPath = "output.mp3";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_result_layout);

        seekBarTime = findViewById(R.id.seekBarTime);
        seekBarVolume = findViewById(R.id.seekBarVolume);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        saveButton = findViewById(R.id.save_button);
        // Lấy đường dẫn từ Intent
        String audioPath = getIntent().getStringExtra("audio_path");
        if (audioPath == null) {
            Toast.makeText(this, "Không tìm thấy file âm thanh!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo MediaPlayer
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi khi tải file âm thanh!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set max cho SeekBar thời gian
        seekBarTime.setMax(mediaPlayer.getDuration());

        // Cập nhật thời gian bài hát
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekBarTime.setProgress(mediaPlayer.getCurrentPosition());
                    runOnUiThread(() -> tvCurrentTime.setText(convertTime(mediaPlayer.getCurrentPosition())));
                }
            }
        }, 0, 1000);

        // Xử lý sự kiện SeekBar thời gian
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Xử lý nút Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayPause.setText("Play");
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setText("Pause");
                }
            }
        });

        // Khởi tạo AudioManager để điều chỉnh âm lượng
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekBarVolume.setMax(maxVolume);
        seekBarVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        // Điều chỉnh âm lượng bằng SeekBar
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        saveButton.setOnClickListener(v -> {
            try {
                saveToRecordings();
                Toast.makeText(this, "Đã lưu file", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Lỗi khi lưu file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Chuyển đổi mili-giây sang định dạng phút:giây
    private String convertTime(int millis) {
        int minutes = millis / 60000;
        int seconds = (millis % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void saveToRecordings() throws IOException {
        String audioFilePath = new File(getExternalFilesDir(null), "output.mp3").getAbsolutePath();
        File sourceFile = new File(audioFilePath);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String songName = getIntent().getStringExtra("song_name");
        String uniqueFileName = songName + "_" + timestamp + ".mp3";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, uniqueFileName);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RECORDINGS);
        }

        Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri);
                 FileInputStream in = new FileInputStream(sourceFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
