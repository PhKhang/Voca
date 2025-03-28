package com.example.voca.ui.record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.example.voca.R;
import com.example.voca.bus.LikeBUS;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.LikeDTO;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.service.FileDownloader;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class RecordActivity extends AppCompatActivity {
    private static final int BUFFER_SIZE = 4096; // Tăng kích thước buffer
    private static final int REQUEST_PERMISSION = 1;
    EditText etMp3Url;
    Button btnDownload;
    TextView tvStatus;
    String songName = "MatKetNoi";
    private String downloadedMp3Path = "https://pub-b0a9bdcea1cd4f6ca28d98f878366466.r2.dev/youtube_LoKtEI9RONw_audio.mp3";
    private String combinedPath = "output.mp3";
    private String backgroundMusicPath = "background_music.mp3";
    private String videoId = "LoKtEI9RONw";
    private static final int REQUEST_RECORD_AUDIO = 1;
    private YouTubePlayer youTubePlayerInstance;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private MediaPlayer onlineMediaPlayer;
    private ByteArrayOutputStream recordingStream;
    private ByteArrayOutputStream musicStream;
    private String mySdPath;

    private boolean isRecording;

    UserBUS userBUS;
    PostBUS postBUS;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.record_room_layout);

        UserBUS userBUS = new UserBUS();
//        userBUS.fetchUsers(new UserBUS.OnUsersFetchedListener() {
//            @Override
//            public void onUsersFetched(List<UserDTO> users) {
//                for (UserDTO user : users) {
//                    System.out.println("User: " + user.getUsername());
//                }
//            }
//            @Override
//            public void onError(String error) {
//                System.err.println("Error: " + error);
//            }
//        });

        PostBUS postBUS = new PostBUS();
        SongBUS songBUS = new SongBUS();
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                for (PostDTO post : posts) {
                    UserDTO user = post.getUser_id();
                    SongDTO song = post.getSong_id();

                    String userId = (user != null) ? user.get_id() : "UNKNOWN";
                    String username = (user != null) ? user.getUsername() : "No Username";
                    String songTitle = (song != null) ? song.getTitle() : "No Title";

                    Log.d("Post", "Caption: " + post.getCaption() +
                            ", User: " + username +
                            " (ID: " + userId + ")" +
                            ", Song: " + songTitle);
                }
            }

            @Override
            public void onError(String error) {
                Log.d("error", error);
            }
        });



// Bước 1: Tạo một người dùng mới
        UserDTO newUser = new UserDTO(null, "firebase_uid_123456", "john_doe4",
                "john4@example.com", "https://example.com/avatar4.jpg",
                "user", null, null, 0);
        userBUS.createUser(newUser, new UserBUS.OnUserCreatedListener() {
            @Override
            public void onUserCreated(UserDTO user) {
                Log.d("UserModule", "Người dùng được tạo: " + user.getUsername() + " với ID: " + user.get_id());

                // Bước 2: Tạo một bài hát do người dùng vừa tạo tải lên
                SongDTO newSong = new SongDTO(null, "youtube_id_45678", "Sample Song 3",
                        "https://example3.com/audio.mp3", "https://example3.com/thumbnail.png", user, null, 0);
                songBUS.createSong(newSong, new SongBUS.OnSongCreatedListener() {
                    @Override
                    public void onSongCreated(SongDTO song) {
                        Log.d("SongModule", "Bài hát được tạo: " + song.getTitle() + " với ID: " + song.get_id());

                        // Bước 3: Tạo bài đăng (dùng nguyên `UserDTO` và `SongDTO`)
                        PostDTO newPost = new PostDTO(null, user, song,
                                "https://example3.com/audio.mp3", "Great song 2!", 0, null);
                        postBUS.createPost(newPost, new PostBUS.OnPostCreatedListener() {
                            @Override
                            public void onPostCreated(PostDTO post) {
                                Log.d("PostModule", "Bài đăng được tạo với ID: " + post.get_id());

                                // Bước 4: Tạo một lượt thích cho bài đăng (Chỉ truyền `post.get_id()`, `user.get_id()`)
                                LikeDTO newLike = new LikeDTO(null, post, user, null);
                                LikeBUS likeBUS = new LikeBUS();
                                likeBUS.createLike(newLike, new LikeBUS.OnLikeCreatedListener() {
                                    @Override
                                    public void onLikeCreated(LikeDTO like) {
                                        Log.d("LikeModule", "Lượt thích được tạo với ID: " + like.get_id());
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e("LikeModule", "Lỗi khi tạo lượt thích: " + error);
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("PostModule", "Lỗi khi tạo bài đăng: " + error);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("SongModule", "Lỗi khi tạo bài hát: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("UserModule", "Lỗi khi tạo người dùng: " + error);
            }
        });

//        Intent intent = getIntent();
//        videoId = intent.getStringExtra("song_id");

        isRecording = false;
        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtube_player_view);
        Button recordButton = findViewById(R.id.record_button);
        Button stopRecordButton = findViewById(R.id.stop_record_button);
//        Button playButton = findViewById(R.id.play_button);
        Button returnButton = findViewById(R.id.return_button);
        Button returnHomeButton = findViewById(R.id.return_home_button);

        getLifecycle().addObserver(youTubePlayerView);

        IFramePlayerOptions options = new IFramePlayerOptions.Builder()
                .controls(0)
                .rel(0)
                .ivLoadPolicy(0)
                .ccLoadPolicy(0)
                .autoplay(0)
                .build();

        youTubePlayerView.setEnableAutomaticInitialization(false);
        saveBackgroundMusic();

        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.cueVideo(videoId, 0);
                youTubePlayerInstance = youTubePlayer;
                youTubePlayer.mute();
            }
        }, true, options);


        recordButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(RecordActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            } else {
                // Kiểm tra file nhạc nền có tồn tại chưa
                File musicFile = new File(getExternalFilesDir(null), backgroundMusicPath);
                if (!musicFile.exists()) {
                    saveBackgroundMusic();
                    Toast.makeText(RecordActivity.this, "Đang tải nhạc nền, vui lòng thử lại...", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isRecording) {
                    if (mediaRecorder == null) {
                        isRecording = true;
                        youTubePlayerInstance.play();
                        startRecording();
                        playLocalFile(musicFile); // Sử dụng file local thay vì stream
                        Toast.makeText(RecordActivity.this, "Đang ghi âm...", Toast.LENGTH_SHORT).show();
                    } else {
                        isRecording = true;
                        youTubePlayerInstance.play();
                        resumeRecording();
                        resumeOnlineAudio();
                        Toast.makeText(RecordActivity.this, "Tiếp tục ghi âm...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    isRecording = false;
                    youTubePlayerInstance.pause();
                    pauseRecording();
                    pauseOnlineAudio();
                    Toast.makeText(RecordActivity.this, "Tạm dừng ghi âm...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stopRecordButton.setOnClickListener(v -> {
            youTubePlayerInstance.pause();
            stopRecording();
            stopOnlineAudio();
            isRecording = false; // Reset trạng thái recording
            Toast.makeText(RecordActivity.this, "Đã kết thúc ghi âm", Toast.LENGTH_SHORT).show();
            playCombinedAudio(new Callback() {
                @Override
                public void onSuccess(String filePath) {
                    Intent intent = new Intent(RecordActivity.this, RecordResultActivity.class);
                    intent.putExtra("audio_path", filePath);
                    intent.putExtra("song_name", songName);
                    startActivity(intent);
                }

                @Override
                public void onFailure() {
                    Toast.makeText(RecordActivity.this, "Không thể phát do lỗi trộn!", Toast.LENGTH_SHORT).show();
                }
            });
        });

//        playButton.setOnClickListener(v -> {
//            // Kiểm tra xem có đang ghi âm không
//            if (isRecording) {
//                Toast.makeText(RecordActivity.this, "Vui lòng dừng ghi âm trước khi phát!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            playCombinedAudio();
//            Intent intent = new Intent(this, RecordResultActivity.class);
//            startActivity(intent);
//        });
//        playButton.setOnClickListener(v -> {
//            if (isRecording) {
//                Toast.makeText(RecordActivity.this, "Vui lòng dừng ghi âm trước khi phát!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            playCombinedAudio(new Callback() {
//                @Override
//                public void onSuccess(String filePath) {
//                    Intent intent = new Intent(RecordActivity.this, RecordResultActivity.class);
//                    intent.putExtra("audio_path", filePath);
//                    intent.putExtra("song_name", songName);
//                    startActivity(intent);
//                }
//
//                @Override
//                public void onFailure() {
//                    Toast.makeText(RecordActivity.this, "Không thể phát do lỗi trộn!", Toast.LENGTH_SHORT).show();
//                }
//            });
//        });

        returnButton.setOnClickListener(v -> {
            youTubePlayerInstance.seekTo(0f);
            youTubePlayerInstance.pause();
            if (isRecording) {
                stopRecording();
                stopOnlineAudio();
                isRecording = false;
            }
            Toast.makeText(RecordActivity.this, "Đã trở về đầu", Toast.LENGTH_SHORT).show();
        });

        returnHomeButton.setOnClickListener(v -> finish());

        audioFilePath = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/audiorecord.m4a";
    }

    private void playLocalFile(File musicFile) {
        if (onlineMediaPlayer == null) {
            onlineMediaPlayer = new MediaPlayer();
        } else {
            onlineMediaPlayer.reset();
        }

        try {
            onlineMediaPlayer.setDataSource(musicFile.getAbsolutePath());
            onlineMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            onlineMediaPlayer.prepare(); // Using synchronous prepare since file is local
            onlineMediaPlayer.start();
            Toast.makeText(RecordActivity.this, "Đang phát nhạc...", Toast.LENGTH_SHORT).show();

            onlineMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                onlineMediaPlayer = null;
            });
        } catch (IOException e) {
            Toast.makeText(RecordActivity.this, "Không thể phát nhạc!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        audioFilePath = new File(getExternalFilesDir(null), "audio_recording.m4a").getAbsolutePath();
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(192000);
        mediaRecorder.setAudioSamplingRate(48000);
        mediaRecorder.setAudioChannels(1);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Toast.makeText(this, "Đang ghi âm...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            releaseMediaRecorder();
            Toast.makeText(this, "Lỗi khi bắt đầu ghi âm", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            Toast.makeText(this, "Lỗi khi thiết lập ghi âm", Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Toast.makeText(this, "Đã dừng ghi âm", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.pause();
        }
    }

    private void resumeRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.resume();
        }
    }

    private void saveBackgroundMusic() {
        String fileName = backgroundMusicPath;
        FileDownloader.downloadExternalFile(this, downloadedMp3Path, fileName);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            stopRecording();
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (onlineMediaPlayer != null && onlineMediaPlayer.isPlaying()) {
            onlineMediaPlayer.release();
            onlineMediaPlayer = null;
        }
    }

    private void stopOnlineAudio() {
        if (onlineMediaPlayer != null) {
            try {
                if (onlineMediaPlayer.isPlaying()) {
                    onlineMediaPlayer.stop();
                    onlineMediaPlayer.reset();
                    onlineMediaPlayer.release(); // Giải phóng tài nguyên
                    onlineMediaPlayer = null; // Set null để có thể tạo instance mới
                    Toast.makeText(RecordActivity.this, "Đã dừng phát nhạc", Toast.LENGTH_SHORT).show();
                }
            } catch (IllegalStateException e) {
                Toast.makeText(RecordActivity.this, "Lỗi khi dừng nhạc!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pauseOnlineAudio() {
        if (onlineMediaPlayer != null && onlineMediaPlayer.isPlaying()) {
            try {
                onlineMediaPlayer.pause();
                Toast.makeText(RecordActivity.this, "Đã tạm dừng", Toast.LENGTH_SHORT).show();
            } catch (IllegalStateException e) {
                Toast.makeText(RecordActivity.this, "Lỗi khi tạm dừng!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resumeOnlineAudio() {
        if (onlineMediaPlayer != null && !onlineMediaPlayer.isPlaying()) {
            try {
                onlineMediaPlayer.start();
                Toast.makeText(RecordActivity.this, "Tiếp tục phát", Toast.LENGTH_SHORT).show();
            } catch (IllegalStateException e) {
                // Nếu MediaPlayer đã bị reset hoặc release, tạo lại và phát từ đầu
                File musicFile = new File(getExternalFilesDir(null), backgroundMusicPath);
                if (musicFile.exists()) {
                    playLocalFile(musicFile);
                } else {
                    Toast.makeText(RecordActivity.this, "Không tìm thấy file nhạc!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private void saveToRecordings() throws IOException {
        String audioFilePath = new File(getExternalFilesDir(null), "output.mp3").getAbsolutePath();
        File sourceFile = new File(audioFilePath);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
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

//    private void playCombinedAudio() {
//        File recordingFile = new File(getExternalFilesDir(null), "audio_recording.m4a");
//        File musicFile = new File(getExternalFilesDir(null), backgroundMusicPath);
//
//        // Kiểm tra nếu file có tồn tại không
//        if (!recordingFile.exists()) {
//            Toast.makeText(this, "Lỗi: Không tìm thấy file ghi âm !", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (!musicFile.exists()) {
//            Toast.makeText(this, "Lỗi: Không tìm thấy file nhạc nền!", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        try {
//            // Đọc file ghi âm vào ByteArrayOutputStream
//            ByteArrayOutputStream recordingStream = new ByteArrayOutputStream();
//            try (FileInputStream fis = new FileInputStream(recordingFile)) {
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = fis.read(buffer)) != -1) {
//                    recordingStream.write(buffer, 0, bytesRead);
//                }
//            }
//
//            // Đọc file nhạc nền vào ByteArrayOutputStream
//            ByteArrayOutputStream musicStream = new ByteArrayOutputStream();
//            try (FileInputStream fis = new FileInputStream(musicFile)) {
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = fis.read(buffer)) != -1) {
//                    musicStream.write(buffer, 0, bytesRead);
//                }
//            }
//
//            // Ghi dữ liệu vào file
//            try (FileOutputStream fos = new FileOutputStream(recordingFile)) {
//                recordingStream.writeTo(fos);
//            }
//            try (FileOutputStream fos = new FileOutputStream(musicFile)) {
//                musicStream.writeTo(fos);
//            }
//        } catch (IOException e) {
//            Toast.makeText(this, "Lỗi khi đọc file âm thanh!", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Trộn âm thanh với FFmpeg
//        String outputFilePath = new File(getExternalFilesDir(null), combinedPath).getAbsolutePath();
//        String command = String.format("-y -i %s -i %s -filter_complex amix=inputs=2:duration=first:dropout_transition=3 %s",
//                recordingFile.getAbsolutePath(), musicFile.getAbsolutePath(), outputFilePath);
//
//        FFmpegKit.executeAsync(command, session -> {
//            if (ReturnCode.isSuccess(session.getReturnCode())) {
////                MediaPlayer mediaPlayer = new MediaPlayer();
////                try {
////                    mediaPlayer.setDataSource(outputFilePath);
////                    mediaPlayer.prepare();
////                    mediaPlayer.start();
////                    Toast.makeText(this, "Đang phát âm thanh kết hợp", Toast.LENGTH_SHORT).show();
////                } catch (IOException e) {
////                    Toast.makeText(this, "Lỗi khi phát âm thanh kết hợp!", Toast.LENGTH_SHORT).show();
////                }
//                File outputFile = new File(outputFilePath);
//                if (outputFile.exists()) {
//                    Toast.makeText(this, "Đã trộn âm thanh thành công!", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(this, "File đầu ra không tạo thành công!", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                Log.e("FFmpegKit", "Command failed with state " + session.getState() + " and return code " + session.getReturnCode());
//                Toast.makeText(this, "Lỗi khi trộn âm thanh!", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
private void playCombinedAudio(Callback callback) {
    if (callback == null) {
        throw new IllegalArgumentException("Callback cannot be null");
    }

    File recordingFile = new File(getExternalFilesDir(null), "audio_recording.m4a");
    File musicFile = new File(getExternalFilesDir(null), backgroundMusicPath);
    File outputFile = new File(getExternalFilesDir(null), combinedPath);
    String outputFilePath = outputFile.getAbsolutePath();

    if (!recordingFile.exists() || !musicFile.exists()) {
        Toast.makeText(this, "File không tồn tại!", Toast.LENGTH_SHORT).show();
        callback.onFailure();
        return;
    }

    String command = String.format("-y -i %s -i %s -filter_complex amix=inputs=2:duration=first:dropout_transition=3 %s",
            recordingFile.getAbsolutePath(), musicFile.getAbsolutePath(), outputFilePath);

    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Đang trộn âm thanh...");
    progressDialog.setCancelable(false);
    progressDialog.show();

    FFmpegKit.executeAsync(command, session -> {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                if (outputFile.exists() && outputFile.length() > 0) {
                    combinedPath = outputFile.getName();
                    callback.onSuccess(outputFilePath);
                } else {
                    Toast.makeText(this, "File đầu ra không hợp lệ!", Toast.LENGTH_SHORT).show();
                    callback.onFailure();
                }
            } else {
                Log.e("FFmpegKit", "Command failed: " + session.getState() + ", Code: " + session.getReturnCode().getValue());
                Toast.makeText(this, "Lỗi khi trộn âm thanh: " + session.getReturnCode().getValue(), Toast.LENGTH_SHORT).show();
                callback.onFailure();
            }
        });
    });
}

    interface Callback {
        void onSuccess(String filePath);
        void onFailure();
    }
}