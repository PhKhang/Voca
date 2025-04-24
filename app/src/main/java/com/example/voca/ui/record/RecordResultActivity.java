package com.example.voca.ui.record;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher; // Needed for caption dialog
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText; // Needed for caption dialog
import android.widget.SeekBar;
import android.widget.TextView;
//import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull; // For NonNull annotation if needed

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session; // Import Session if using its methods directly
import com.arthenica.ffmpegkit.SessionState;
import com.example.voca.R; // Ensure this matches your R file
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.service.FileUploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat; // For formatting delay display
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects; // For Objects.requireNonNull if used

public class RecordResultActivity extends AppCompatActivity {
    private static final String TAG = "RecordResultActivity_FFmpeg";

    // UI Elements
    private SeekBar seekBarTime, seekBarVolume, seekBarEcho, seekBarDelay, seekBarBass, seekBarTreble;
    private TextView tvCurrentTime, tvSongNameResult;
    private Button btnPlayPause, btnConfirmAndMix, saveButton, btnCreatePost;
    // Sync delay controls
    private SeekBar seekBarSyncDelay;
    private TextView tvSyncDelayValue;

    // File Paths
    private String originalRecordingPath;
    private String backgroundMusicPath;
    private String finalMixedAudioPath; // Path to the FINAL mixed file
    private String songName;
    private String songId;

    // Media Player
    private MediaPlayer mediaPlayer;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    // FFmpeg filter parameters
    private float currentVolume = 1.0f;
    private float currentEchoDecay = 0.0f;
    private int currentEchoDelay = 0;
    private int currentBassGain = 0;
    private int currentTrebleGain = 0;
    private int voiceDelayMs = 0; // Updated by seekBarSyncDelay (still in ms for FFmpeg)

    // Business Logic Handlers
    private UserBUS userBUS;
    private SongBUS songBUS;
    private PostBUS postBUS;

    // Constants for Sync Delay SeekBar (UPDATED FOR +/- 2 seconds)
    private static final int SYNC_DELAY_MAX_PROGRESS = 40;  // Total steps (0 to 40 for -2s to +2s)
    private static final int SYNC_DELAY_ZERO_OFFSET = 20;   // Middle point (0.0s -> progress 20)
    private static final float SYNC_DELAY_STEP = 0.1f;      // Step interval (0.1 seconds)

    // Format for displaying delay value (e.g., "+1.2 s")
    private DecimalFormat delayFormatter = new DecimalFormat("+#,##0.0 s;-#,##0.0 s");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide action bar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.record_result_layout);

        // Initialize BUS objects
        userBUS = new UserBUS();
        songBUS = new SongBUS();
        postBUS = new PostBUS();

        // --- Get Intent Extras and Validation (Robust) ---
        try {
            originalRecordingPath = getIntent().getStringExtra("recording_path");
            backgroundMusicPath = getIntent().getStringExtra("background_music_path");
            songName = getIntent().getStringExtra("song_name");
            songId = getIntent().getStringExtra("song_id");

            if (originalRecordingPath == null || backgroundMusicPath == null || songName == null || songId == null) {
                throw new IllegalArgumentException("Missing required intent extras");
            }

            File originalFile = new File(originalRecordingPath);
            File musicFile = new File(backgroundMusicPath);
            if (!originalFile.exists() || !originalFile.isFile()) {
                throw new IOException("Original recording file invalid: " + originalRecordingPath);
            }
            if (!musicFile.exists() || !musicFile.isFile()) {
                throw new IOException("Background music file invalid: " + backgroundMusicPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting/validating intent extras", e);
            //Toast.makeText(this, "Lỗi tải dữ liệu. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            finish(); // Exit activity if essential data is missing/invalid
            return;
        }

        // --- Initialize UI Elements ---
        findViews(); // Ensure views are found before use
        tvSongNameResult.setText(songName);

        // --- Setup Listeners ---
        setupSeekBarListeners();
        setupButtonListeners();

        // --- Initialize Media Player ---
        mediaPlayer = new MediaPlayer();
        setupMediaPlayerListeners();

        // --- Set Initial State ---
        setPlaybackControlsEnabled(false); // Playback controls disabled initially
        btnConfirmAndMix.setEnabled(true); // Confirm button enabled initially
        updateEffectParametersFromSeekBars(); // Read initial effect SeekBar values
        // Set initial sync delay display based on default progress
        if (seekBarSyncDelay != null) { // Ensure SeekBar exists before getting progress
            updateSyncDelayDisplay(seekBarSyncDelay.getProgress());
        }
    }

    private void findViews() {
        try {
            seekBarTime = findViewById(R.id.seekBarTime);
            seekBarVolume = findViewById(R.id.seekBarVolume);
            seekBarEcho = findViewById(R.id.seekBarEcho); // Echo Decay %
            seekBarDelay = findViewById(R.id.seekBarDelay); // Echo Delay Time (ms)
            seekBarBass = findViewById(R.id.seekBarBass);
            seekBarTreble = findViewById(R.id.seekBarTreble);
            tvCurrentTime = findViewById(R.id.tvCurrentTime);
            tvSongNameResult = findViewById(R.id.tvSongNameResult);
            btnPlayPause = findViewById(R.id.btnPlayPause);
            btnConfirmAndMix = findViewById(R.id.btnApplyEffects); // Assumed ID in XML
            saveButton = findViewById(R.id.save_button);
            btnCreatePost = findViewById(R.id.btn_create_post);
            seekBarSyncDelay = findViewById(R.id.seekBarSyncDelay); // Sync Delay SeekBar
            tvSyncDelayValue = findViewById(R.id.tvSyncDelayValue); // Sync Delay TextView

            // Add null checks for all essential views
            Objects.requireNonNull(seekBarTime, "seekBarTime is null");
            Objects.requireNonNull(seekBarVolume, "seekBarVolume is null");
            Objects.requireNonNull(seekBarEcho, "seekBarEcho is null");
            Objects.requireNonNull(seekBarDelay, "seekBarDelay is null");
            Objects.requireNonNull(seekBarBass, "seekBarBass is null");
            Objects.requireNonNull(seekBarTreble, "seekBarTreble is null");
            Objects.requireNonNull(tvCurrentTime, "tvCurrentTime is null");
            Objects.requireNonNull(tvSongNameResult, "tvSongNameResult is null");
            Objects.requireNonNull(btnPlayPause, "btnPlayPause is null");
            Objects.requireNonNull(btnConfirmAndMix, "btnConfirmAndMix is null");
            Objects.requireNonNull(saveButton, "saveButton is null");
            Objects.requireNonNull(btnCreatePost, "btnCreatePost is null");
            Objects.requireNonNull(seekBarSyncDelay, "seekBarSyncDelay is null");
            Objects.requireNonNull(tvSyncDelayValue, "tvSyncDelayValue is null");

            btnConfirmAndMix.setText("Xác nhận & Trộn");

        } catch (NullPointerException e) {
            Log.e(TAG, "Fatal Error: One or more essential views not found in layout.", e);
            //Toast.makeText(this, "Lỗi nghiêm trọng: Giao diện không tải đúng.", Toast.LENGTH_LONG).show();
            finish(); // Exit if UI is broken
        }
    }

    // Reads current progress from effect SeekBars and updates corresponding variables
    private void updateEffectParametersFromSeekBars() {
        // Add null checks before accessing progress (although findViews should catch this)
        if (seekBarVolume != null) currentVolume = seekBarVolume.getProgress() / 100.0f;
        if (seekBarEcho != null) currentEchoDecay = seekBarEcho.getProgress() / 100.0f;
        if (seekBarDelay != null) currentEchoDelay = seekBarDelay.getProgress();
        if (seekBarBass != null) currentBassGain = seekBarBass.getProgress() - 10;
        if (seekBarTreble != null) currentTrebleGain = seekBarTreble.getProgress() - 10;
    }

    // Attaches listeners to all SeekBars
    private void setupSeekBarListeners() {
        // Listener for effect SeekBars (Volume, Echo, Bass, Treble)
        OnSeekBarChangeListener effectChangeListener = new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateEffectParametersFromSeekBars();
                    // No immediate action needed, effects applied on confirm
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        // Add null checks before setting listeners
        if (seekBarVolume != null) seekBarVolume.setOnSeekBarChangeListener(effectChangeListener);
        if (seekBarEcho != null) seekBarEcho.setOnSeekBarChangeListener(effectChangeListener); // Echo Decay
        if (seekBarDelay != null) seekBarDelay.setOnSeekBarChangeListener(effectChangeListener); // Echo Delay Time
        if (seekBarBass != null) seekBarBass.setOnSeekBarChangeListener(effectChangeListener);
        if (seekBarTreble != null) seekBarTreble.setOnSeekBarChangeListener(effectChangeListener);

        // Listener for playback time SeekBar (seekBarTime)
        if (seekBarTime != null) {
            seekBarTime.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                int seekPosition = 0;
                boolean wasPlaying = false;

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        seekPosition = progress;
                        // Update time display immediately while scrubbing
                        String current = formatTime(progress);
                        String total = (mediaPlayer != null && isMediaPlayerPrepared())
                                ? formatTime(mediaPlayer.getDuration()) : "00:00";
                        if (tvCurrentTime != null) tvCurrentTime.setText(String.format("%s / %s", current, total));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    if (mediaPlayer != null && isMediaPlayerPrepared()) {
                        try {
                            wasPlaying = mediaPlayer.isPlaying();
                            if (wasPlaying) {
                                mediaPlayer.pause(); // Pause playback while seeking
                                stopUpdatingProgress();
                                if (btnPlayPause != null) btnPlayPause.setText("Play");
                            }
                        } catch (IllegalStateException e) {
                            Log.w(TAG, "MediaPlayer state error on startTrackingTouch", e);
                            wasPlaying = false; // Assume not playing if state is invalid
                        }
                    } else {
                        wasPlaying = false;
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mediaPlayer != null && isMediaPlayerPrepared()) {
                        try {
                            int duration = mediaPlayer.getDuration();
                            // Ensure seek position is valid
                            if (seekPosition >= duration) {
                                seekPosition = duration > 100 ? duration - 100 : 0; // Seek near end or start
                            }
                            if (seekPosition < 0) seekPosition = 0;

                            mediaPlayer.seekTo(seekPosition);
                            // Update display after seeking
                            String current = formatTime(seekPosition);
                            String total = formatTime(duration);
                            if (tvCurrentTime != null) tvCurrentTime.setText(String.format("%s / %s", current, total));

                            // Resume playback if it was playing before
                            if (wasPlaying) {
                                mediaPlayer.start();
                                startUpdatingProgress();
                                if (btnPlayPause != null) btnPlayPause.setText("Pause");
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "IllegalStateException during seekTo after stopTrackingTouch", e);
                            // Handle error, e.g., reset player or show message
                            resetMediaPlayer();
                            setPlaybackControlsEnabled(false);
                            //Toast.makeText(RecordResultActivity.this, "Lỗi khi tua, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        // Listener for the Sync Delay SeekBar (seekBarSyncDelay)
        if (seekBarSyncDelay != null) {
            seekBarSyncDelay.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        // Update the internal variable and the display TextView
                        updateSyncDelayDisplay(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                } // Value updated in onProgressChanged
            });
        }
    }

    // Updates the voiceDelayMs variable and the tvSyncDelayValue TextView
    // based on the progress of the seekBarSyncDelay
    private void updateSyncDelayDisplay(int progress) {
        // Map progress (0-40 for +/- 2s) to delay in seconds
        float voiceDelaySeconds = (progress - SYNC_DELAY_ZERO_OFFSET) * SYNC_DELAY_STEP;

        // Convert seconds to milliseconds for FFmpeg, rounding appropriately
        voiceDelayMs = Math.round(voiceDelaySeconds * 1000);

        // Format the string for display (e.g., "+1.2 s", "0.0 s", "-0.8 s")
        // Use the pre-initialized delayFormatter
        String delayString = delayFormatter.format(voiceDelaySeconds);

        // Update the TextView safely
        if (tvSyncDelayValue != null) {
            tvSyncDelayValue.setText(delayString);
        }
        // Optional Log for debugging
        // Log.d(TAG, "Sync Progress: " + progress + " -> Delay: " + voiceDelaySeconds + " s (" + voiceDelayMs + " ms)");
    }

    // Helper to check if MediaPlayer is in a prepared or playing state
    private boolean isMediaPlayerPrepared() {
        if (mediaPlayer == null) return false;
        try {
            // Calling getDuration() throws IllegalStateException if not prepared/valid
            mediaPlayer.getDuration();
            return true;
        } catch (IllegalStateException e) {
            return false; // Expected if not prepared
        } catch (Exception e) { // Catch any other unexpected errors
            Log.w(TAG, "Unexpected error checking media player state", e);
            return false;
        }
    }

    // Attaches OnClickListeners to buttons
    private void setupButtonListeners() {
        // Add null checks before setting listeners
        if (btnConfirmAndMix != null) {
            btnConfirmAndMix.setOnClickListener(v -> createFinalMixedAudio());
        }
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> togglePlayback());
        }
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                if (finalMixedAudioPath != null && new File(finalMixedAudioPath).exists()) {
                    saveFinalAudio(); // Call dedicated save method
                } else {
                    //Toast.makeText(this, "Chưa có bản thu hoàn chỉnh để lưu", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (btnCreatePost != null) {
            btnCreatePost.setOnClickListener(v -> {
                if (finalMixedAudioPath != null && new File(finalMixedAudioPath).exists()) {
                    showCreatePostDialog();
                } else {
                    //Toast.makeText(this, "Chưa có bản thu hoàn chỉnh để đăng", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Shows dialog for entering post caption
    private void showCreatePostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText inputCaption = dialogView.findViewById(R.id.inputCaption);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnPost = dialogView.findViewById(R.id.btnPost);

        inputCaption.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                btnPost.setEnabled(s != null && s.toString().trim().length() > 0);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPost.setOnClickListener(v -> {
            String caption = inputCaption.getText().toString().trim();
            if (!caption.isEmpty()) {
                if (finalMixedAudioPath != null && new File(finalMixedAudioPath).exists()) {
                    uploadAndCreatePost(finalMixedAudioPath, caption);
                    dialog.dismiss();
                } else {
                    //Toast.makeText(RecordResultActivity.this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                    // Toast.makeText(this, "Lỗi: File âm thanh không còn tồn tại.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // Handles uploading the audio file and creating the post via BUS layers
    private void uploadAndCreatePost(String filePath, String caption) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            //Toast.makeText(this, "Lỗi: File âm thanh không tìm thấy để tải lên.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải lên và tạo bài đăng...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        FileUploader fileUploader = new FileUploader();
        fileUploader.run(this, Uri.fromFile(file), new FileUploader.OnUploadCompleteListener() {
            @Override
            public void onSuccess(String audioUrl) {
                fetchUserAndSongThenCreatePost(audioUrl, caption, progressDialog);
            }

            @Override
            public void onFailure() {
                handlePostCreationError("Lỗi khi tải file âm thanh lên!", progressDialog);
            }
        });
    }

    // Helper method to chain the async calls for user/song fetch and post creation
    private void fetchUserAndSongThenCreatePost(String audioUrl, String caption, ProgressDialog progressDialog) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId == null) {
            handlePostCreationError("Không tìm thấy ID người dùng.", progressDialog);
            return;
        }

        userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
            @Override
            public void onUserFetched(UserDTO user) {
                songBUS.fetchSongById(songId, new SongBUS.OnSongFetchedListener() {
                    @Override
                    public void onSongFetched(SongDTO song) {
                        createPostNow(user, song, audioUrl, caption, progressDialog);
                    }
                    @Override
                    public void onError(String error) {
                        handlePostCreationError("Lỗi lấy thông tin bài hát: " + error, progressDialog);
                    }
                });
            }
            @Override
            public void onError(String error) {
                handlePostCreationError("Lỗi lấy thông tin người dùng: " + error, progressDialog);
            }
        });
    }

    // Performs the actual post creation API call
    private void createPostNow(UserDTO user, SongDTO song, String audioUrl, String caption, ProgressDialog progressDialog) {
        PostDTO newPost = new PostDTO(null, user, song, audioUrl, caption, 0, null);
        postBUS.createPost(newPost, new PostBUS.OnPostCreatedListener() {
            @Override
            public void onPostCreated(PostDTO post) {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    //Toast.makeText(RecordResultActivity.this, "Đã tạo bài đăng thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Close this activity
                });
            }
            @Override
            public void onError(String error) {
                handlePostCreationError("Lỗi khi tạo bài đăng: " + error, progressDialog);
            }
        });
    }

    // Centralized error handler for the post creation process
    private void handlePostCreationError(String errorMessage, ProgressDialog progressDialog) {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Log.e(TAG, "Post Creation Error: " + errorMessage);
            //Toast.makeText(RecordResultActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }


    // Increments the recorded_people count for the song


    // Sets up MediaPlayer listeners for prepared, completion, and error events
    private void setupMediaPlayerListeners() {
        // Ensure mediaPlayer is not null
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            Log.w(TAG, "MediaPlayer was null in setupMediaPlayerListeners, re-initialized.");
        }

        mediaPlayer.setOnPreparedListener(mp -> {
            Log.d(TAG, "MediaPlayer prepared. Duration: " + mp.getDuration());
            if (mp.getDuration() <= 0) {
                Log.w(TAG, "MediaPlayer prepared with invalid duration. Resetting.");
                //Toast.makeText(this, "Lỗi: File âm thanh không hợp lệ.", Toast.LENGTH_SHORT).show();
                resetMediaPlayer();
                setPlaybackControlsEnabled(false);
                return;
            }
            if (seekBarTime != null) seekBarTime.setMax(mp.getDuration());
            setPlaybackControlsEnabled(true);
            String total = formatTime(mp.getDuration());
            if (tvCurrentTime != null) tvCurrentTime.setText(String.format("00:00 / %s", total));
            try {
                mp.start();
                if (btnPlayPause != null) btnPlayPause.setText("Pause");
                startUpdatingProgress();
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException on MediaPlayer start", e);
                resetMediaPlayer();
                setPlaybackControlsEnabled(false);
            }
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "MediaPlayer completion.");
            if (mp != null && isMediaPlayerPrepared()) {
                if (btnPlayPause != null) btnPlayPause.setText("Play");
                try {
                    int duration = mp.getDuration();
                    if (seekBarTime != null) seekBarTime.setProgress(duration);
                    if (tvCurrentTime != null) tvCurrentTime.setText(String.format("%1$s / %1$s", formatTime(duration)));
                } catch (IllegalStateException e) {
                    Log.w(TAG, "IllegalStateException on completion, resetting UI", e);
                    if (seekBarTime != null) seekBarTime.setProgress(0);
                    if (tvCurrentTime != null) tvCurrentTime.setText("00:00 / 00:00");
                }
            } else {
                if (btnPlayPause != null) btnPlayPause.setText("Play");
                if (seekBarTime != null) seekBarTime.setProgress(0);
                if (tvCurrentTime != null) tvCurrentTime.setText("00:00 / 00:00");
            }
            stopUpdatingProgress();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);
            String errorMsg = "Lỗi không xác định khi phát nhạc (what=" + what + ", extra=" + extra +")";
            // ... (More detailed error messages based on 'what' and 'extra' codes - see previous example) ...
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED: errorMsg = "Lỗi kết nối máy chủ media."; break;
                case MediaPlayer.MEDIA_ERROR_UNKNOWN: errorMsg = "Lỗi không xác định khi phát nhạc."; break;
            }
            switch (extra) {
                case MediaPlayer.MEDIA_ERROR_IO: errorMsg = "Lỗi đọc file âm thanh."; break;
                case MediaPlayer.MEDIA_ERROR_MALFORMED: errorMsg = "File âm thanh bị lỗi hoặc không hỗ trợ."; break;
                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED: errorMsg = "Định dạng âm thanh không được hỗ trợ."; break;
                case MediaPlayer.MEDIA_ERROR_TIMED_OUT: errorMsg = "Quá thời gian chờ khi phát."; break;
            }

            //Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            resetMediaPlayer();
            setPlaybackControlsEnabled(false);
            return true; // Error handled
        });
    }

    // Core function to run FFmpeg for mixing audio
    // Core function to run FFmpeg for mixing audio
    private void createFinalMixedAudio() {
        // voiceDelayMs is updated by the SeekBar listener
        Log.d(TAG, "Using voice delay: " + voiceDelayMs + " ms"); // Check the final ms value

        // --- Input File Validation ---
        if (originalRecordingPath == null || backgroundMusicPath == null
                || !new File(originalRecordingPath).exists() || !new File(backgroundMusicPath).exists()) {
           // Toast.makeText(this, "Lỗi: File đầu vào không hợp lệ hoặc không tồn tại.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get latest effect parameters from SeekBars
        updateEffectParametersFromSeekBars();

        // --- Output File Setup ---
        String outputFileName = "final_mix_" + System.currentTimeMillis() + ".mp3";
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir == null) {
            //Toast.makeText(this, "Lỗi: Không thể truy cập bộ nhớ tạm.", Toast.LENGTH_SHORT).show();
            return;
        }
        File outputFile = new File(externalFilesDir, outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();

        // --- Build FFmpeg Filter Complex Graph ---
        StringBuilder filterGraph = new StringBuilder();
        String voiceInput = "[0:a]"; // Input stream from the first file (voice recording)
        String musicInput = "[1:a]"; // Input stream from the second file (background music)
        String currentVoiceLabel = voiceInput; // Label for the current state of the voice stream
        String currentMusicLabel = musicInput; // Label for the current state of the music stream

        // 1. Apply Voice Effects (Sequentially, updating currentVoiceLabel)
        filterGraph.append(String.format(Locale.US, "%s volume=volume=%.2f [vol_out];", currentVoiceLabel, currentVolume));
        currentVoiceLabel = "[vol_out]"; // Update label after volume filter

        if (Math.abs(currentBassGain) > 0.1) {
            filterGraph.append(String.format(Locale.US, "%s bass=g=%d:f=100:w=0.5 [bass_out];", currentVoiceLabel, currentBassGain));
            currentVoiceLabel = "[bass_out]"; // Update label after bass filter
        }
        if (Math.abs(currentTrebleGain) > 0.1) {
            filterGraph.append(String.format(Locale.US, "%s treble=g=%d:f=3000:w=0.5 [treble_out];", currentVoiceLabel, currentTrebleGain));
            currentVoiceLabel = "[treble_out]"; // Update label after treble filter
        }
        if (currentEchoDelay > 0 && currentEchoDecay > 0.01) {
            // Use correct aecho syntax: delays=...:decays=...
            filterGraph.append(String.format(Locale.US, "%s aecho=delays=%d:decays=%.2f [echo_out];", currentVoiceLabel, currentEchoDelay, currentEchoDecay));
            currentVoiceLabel = "[echo_out]"; // Update label only if echo is applied
        }
        // currentVoiceLabel now holds the label of the fully processed voice stream

        // 2. Apply FIXED Volume Reduction to Background Music
        String reducedMusicLabel = "[music_reduced]"; // Label for music after volume reduction
        filterGraph.append(String.format(Locale.US, "%s volume=volume=%.1f %s;", currentMusicLabel, 0.7f, reducedMusicLabel));
        currentMusicLabel = reducedMusicLabel; // Update music label

        // 3. Apply Synchronization Delay (adelay) if needed
        String voiceStreamForMix = currentVoiceLabel; // Default voice stream for mixing
        String musicStreamForMix = currentMusicLabel; // Default music stream for mixing

        if (voiceDelayMs > 0) { // Voice is late -> Delay the final voice stream
            String delayedVoiceLabel = "[delayed_voice]";
            // Apply delay using the correct syntax (delay|delay)
            filterGraph.append(String.format(Locale.US, "%s adelay=%d|%d %s;", voiceStreamForMix, voiceDelayMs, voiceDelayMs, delayedVoiceLabel));
            voiceStreamForMix = delayedVoiceLabel; // Update the voice stream label for mixing
        } else if (voiceDelayMs < 0) { // Voice is early -> Delay the final music stream
            int musicDelayMs = Math.abs(voiceDelayMs);
            String delayedMusicLabel = "[delayed_music]";
            // Apply delay using the correct syntax (delay|delay)
            filterGraph.append(String.format(Locale.US, "%s adelay=%d|%d %s;", musicStreamForMix, musicDelayMs, musicDelayMs, delayedMusicLabel));
            musicStreamForMix = delayedMusicLabel; // Update the music stream label for mixing
        }
        // If voiceDelayMs == 0, voiceStreamForMix and musicStreamForMix remain unchanged

        // 4. Mix the final voice and music streams
        // Use the potentially delayed stream labels (voiceStreamForMix, musicStreamForMix)
        filterGraph.append(String.format(Locale.US, "%s %s amix=inputs=2:duration=first:dropout_transition=3 [aout]",
                voiceStreamForMix, musicStreamForMix));

        // --- Construct FFmpeg Command ---
        String ffmpegCommand = String.format(Locale.US,
                "-y -i \"%s\" -i \"%s\" -filter_complex \"%s\" -map \"[aout]\" -c:a libmp3lame -q:a 2 \"%s\"",
                originalRecordingPath, backgroundMusicPath, filterGraph.toString(), outputFilePath
        );
        // Log the filtergraph for easier debugging
        Log.i(TAG, "Filtergraph:\n" + filterGraph.toString());
        Log.i(TAG, "Executing FFmpeg command with delay (" + voiceDelayMs + "ms):\n" + ffmpegCommand);

        // --- Execute FFmpeg Asynchronously ---
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang hoàn thiện bản thu...");
        progressDialog.setCancelable(false); // Prevent cancellation during FFmpeg execution
        progressDialog.show();

        setExecutionInProgress(true); // Disable UI controls

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            final SessionState state = session.getState();
            final ReturnCode returnCode = session.getReturnCode();

            // Ensure UI updates happen on the main thread
            runOnUiThread(() -> {
                // Safely dismiss the progress dialog
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                setExecutionInProgress(false); // Re-enable UI controls

                if (ReturnCode.isSuccess(returnCode)) {
                    Log.i(TAG, "FFmpeg final mix completed successfully.");
                    File resultFile = new File(outputFilePath);
                    // Verify the output file exists and has content
                    if (resultFile.exists() && resultFile.length() > 0) {
                        //Toast.makeText(RecordResultActivity.this, "Hoàn tất trộn âm thanh!", Toast.LENGTH_SHORT).show();
                        deletePreviousFinalFile(); // Clean up any old mix file
                        finalMixedAudioPath = outputFilePath; // Store the path to the new file
                        prepareAndPlayMediaPlayer(finalMixedAudioPath); // Prepare and start playback
                    } else {
                        // Handle case where FFmpeg reports success but file is invalid
                        Log.e(TAG, "FFmpeg success reported but output file is invalid: " + outputFilePath);
                        //Toast.makeText(RecordResultActivity.this, "Lỗi: Không tạo được file âm thanh cuối cùng.", Toast.LENGTH_LONG).show();
                        finalMixedAudioPath = null; // Invalidate the path
                        setPlaybackControlsEnabled(false); // Disable playback controls
                    }
                } else {
                    // Handle FFmpeg execution failure
                    Log.e(TAG, String.format("FFmpeg process failed! State: %s, RC: %s", state, returnCode));
                    // Log the full FFmpeg output - CRITICAL for debugging
                    Log.e(TAG, "FFmpeg Full Output Logs:\n" + session.getAllLogsAsString());
                    // Provide a user-friendly error message including the return code
                    String errorDetail = returnCode != null ? returnCode.toString() : "Unknown Error";
                    //Toast.makeText(RecordResultActivity.this, "Lỗi khi xử lý âm thanh (" + errorDetail + "). Vui lòng kiểm tra log.", Toast.LENGTH_LONG).show();
                    finalMixedAudioPath = null; // Invalidate the path on failure
                    resetMediaPlayer(); // Reset the media player state
                    setPlaybackControlsEnabled(false); // Disable playback controls
                }
            });
        });
    } // End of createFinalMixedAudio

    // Deletes the previously generated final mixed audio file
    private void deletePreviousFinalFile() {
        if (finalMixedAudioPath != null) {
            File previousFile = new File(finalMixedAudioPath);
            if (previousFile.exists() && previousFile.isFile()) {
                if (previousFile.delete()) {
                    Log.i(TAG, "Deleted previous final mixed file: " + finalMixedAudioPath);
                } else {
                    Log.w(TAG, "Failed to delete previous final mixed file: " + finalMixedAudioPath);
                }
            }
            finalMixedAudioPath = null; // Clear ref
        }
    }

    // --- Media Player Controls ---
    private void togglePlayback() {
        if (mediaPlayer == null || finalMixedAudioPath == null) {
           // Toast.makeText(this, "Chưa có bản thu hoàn chỉnh.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (!isMediaPlayerPrepared()) {
                if(new File(finalMixedAudioPath).exists()){
                    Log.w(TAG, "Player not prepared, attempting prepare...");
                    prepareAndPlayMediaPlayer(finalMixedAudioPath);
                } else {
                   // Toast.makeText(this, "File âm thanh không tồn tại.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (mediaPlayer.isPlaying()) {
                pauseMediaPlayer();
            } else {
                mediaPlayer.start();
                if (btnPlayPause != null) btnPlayPause.setText("Pause");
                startUpdatingProgress();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer state error during togglePlayback", e);
           // Toast.makeText(this, "Lỗi trình phát, đang thử lại...", Toast.LENGTH_SHORT).show();
            prepareAndPlayMediaPlayer(finalMixedAudioPath);
        }
    }

    private void prepareAndPlayMediaPlayer(@NonNull String filePath) {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        File file = new File(filePath);
        if (!file.exists() || !file.isFile() || file.length() == 0) {
            Log.e(TAG, "prepareAndPlayMediaPlayer: File is invalid: " + filePath);
            //Toast.makeText(this, "File âm thanh không hợp lệ!", Toast.LENGTH_SHORT).show();
            finalMixedAudioPath = null;
            setPlaybackControlsEnabled(false);
            return;
        }
        Log.i(TAG, "Preparing MediaPlayer for: " + filePath);
        resetMediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync();
            if (btnPlayPause != null) btnPlayPause.setText("Loading...");
            setPlaybackControlsEnabled(false);
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG, "Error setting data source or preparing", e);
            //Toast.makeText(this, "Lỗi tải file âm thanh: " + e.getMessage(), Toast.LENGTH_LONG).show();
            resetMediaPlayer();
            setPlaybackControlsEnabled(false);
        }
    }

    private void pauseMediaPlayer() {
        if (mediaPlayer != null && isMediaPlayerPrepared() && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                if (btnPlayPause != null) btnPlayPause.setText("Play");
                stopUpdatingProgress();
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException during pause", e);
                resetMediaPlayer();
                setPlaybackControlsEnabled(false);
            }
        }
    }

    private void resetMediaPlayer() {
        if (mediaPlayer != null) {
            stopUpdatingProgress();
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                Log.d(TAG, "MediaPlayer reset.");
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException during reset/stop. Releasing/recreating.", e);
                try { mediaPlayer.release(); } catch (Exception ignored) {}
                mediaPlayer = new MediaPlayer();
                setupMediaPlayerListeners();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during MediaPlayer reset/stop", e);
            }
        } else {
            mediaPlayer = new MediaPlayer();
            setupMediaPlayerListeners();
        }
        // Reset UI
        if (seekBarTime != null) seekBarTime.setProgress(0);
        if (tvCurrentTime != null) tvCurrentTime.setText("00:00 / 00:00");
        if (btnPlayPause != null) btnPlayPause.setText("Play");
        setPlaybackControlsEnabled(false);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            stopUpdatingProgress();
            try {
                // Check state before release is generally not needed, but can prevent rare issues
                // if (isMediaPlayerPrepared() || mediaPlayer.isPlaying()) {
                //     mediaPlayer.stop();
                // }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG,"Exception during MediaPlayer release", e);
            }
            mediaPlayer = null;
            Log.i(TAG, "MediaPlayer released.");
        }
    }

    // --- Progress Update ---
    private void startUpdatingProgress() {
        if (mediaPlayer == null || progressHandler == null) return;
        stopUpdatingProgress();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mediaPlayer != null && isMediaPlayerPrepared() && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int duration = mediaPlayer.getDuration();
                        if (duration <= 0) { stopUpdatingProgress(); return; }
                        currentPosition = Math.max(0, Math.min(currentPosition, duration)); // Clamp position

                        if (seekBarTime != null) seekBarTime.setProgress(currentPosition);
                        String current = formatTime(currentPosition);
                        String total = formatTime(duration);
                        if (tvCurrentTime != null) tvCurrentTime.setText(String.format("%s / %s", current, total));

                        progressHandler.postDelayed(this, 500);
                    } else {
                        stopUpdatingProgress();
                    }
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Error updating progress (MediaPlayer state): " + e.getMessage());
                    stopUpdatingProgress();
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error updating progress", e);
                    stopUpdatingProgress();
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopUpdatingProgress() {
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    // --- Utility Functions ---
    private String formatTime(int milliseconds) {
        if (milliseconds < 0) milliseconds = 0;
        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    // Enables/disables playback-related controls
    private void setPlaybackControlsEnabled(boolean enabled) {
        boolean canEnable = enabled && (finalMixedAudioPath != null && new File(finalMixedAudioPath).exists());

        if (btnPlayPause != null) btnPlayPause.setEnabled(canEnable);
        if (saveButton != null) saveButton.setEnabled(canEnable);
        if (btnCreatePost != null) btnCreatePost.setEnabled(canEnable);
        if (seekBarTime != null) seekBarTime.setEnabled(canEnable);

        if (canEnable && mediaPlayer != null && isMediaPlayerPrepared()) {
            try {
                if (btnPlayPause != null) btnPlayPause.setText(mediaPlayer.isPlaying() ? "Pause" : "Play");
            } catch (IllegalStateException e) {
                if (btnPlayPause != null) btnPlayPause.setText("Play");
            }
        } else if (!canEnable && btnPlayPause != null) {
            btnPlayPause.setText("Play");
        }
    }

    // Enables/disables controls during FFmpeg execution
    private void setExecutionInProgress(boolean inProgress) {
        if (btnConfirmAndMix != null) btnConfirmAndMix.setEnabled(!inProgress);
        if (seekBarSyncDelay != null) seekBarSyncDelay.setEnabled(!inProgress);
        // Disable effect SeekBars during processing
        if (seekBarVolume != null) seekBarVolume.setEnabled(!inProgress);
        if (seekBarEcho != null) seekBarEcho.setEnabled(!inProgress);
        if (seekBarDelay != null) seekBarDelay.setEnabled(!inProgress);
        if (seekBarBass != null) seekBarBass.setEnabled(!inProgress);
        if (seekBarTreble != null) seekBarTreble.setEnabled(!inProgress);

        if (inProgress) {
            setPlaybackControlsEnabled(false);
            if (mediaPlayer != null && isMediaPlayerPrepared() && mediaPlayer.isPlaying()) {
                pauseMediaPlayer();
            }
        }
    }

    // --- File Saving ---
    private void saveFinalAudio() {
        if (finalMixedAudioPath == null || !new File(finalMixedAudioPath).exists()) {
            //Toast.makeText(this, "Lỗi: Không tìm thấy file âm thanh để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            saveToRecordings(finalMixedAudioPath);
            //Toast.makeText(this, "Đã lưu bản thu thành công!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error saving final mixed file", e);
            //Toast.makeText(this, "Lỗi khi lưu file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error saving file", e);
            //Toast.makeText(this, "Lỗi quyền truy cập bộ nhớ khi lưu file.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveToRecordings(@NonNull String sourceFilePath) throws IOException, SecurityException {
        Objects.requireNonNull(sourceFilePath, "sourceFilePath cannot be null");
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException("Source file invalid: " + sourceFilePath);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String baseName = (songName != null && !songName.isEmpty()) ? songName : "VocaRecording";
        baseName = baseName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        String uniqueFileName = baseName + "_Mix_" + timestamp + ".mp3";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, uniqueFileName);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg");
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);

        Uri collectionUri;
        String relativePath = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            relativePath = Environment.DIRECTORY_RECORDINGS;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath);
            collectionUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            File recordingsDir = Environment.getExternalStoragePublicDirectory(relativePath);
            if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
                throw new IOException("Cannot create Recordings directory: " + recordingsDir.getAbsolutePath());
            }
            if (!recordingsDir.isDirectory() || !recordingsDir.canWrite()) {
                throw new IOException("Recordings directory invalid or not writable: " + recordingsDir.getAbsolutePath());
            }
            File targetFile = new File(recordingsDir, uniqueFileName);
            values.put(MediaStore.Audio.Media.DATA, targetFile.getAbsolutePath());
            collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        Uri itemUri = null;
        OutputStream out = null;
        FileInputStream in = null;
        try {
            itemUri = getContentResolver().insert(collectionUri, values);
            if (itemUri == null) {
                throw new IOException("Failed to create MediaStore entry for " + uniqueFileName);
            }
            out = getContentResolver().openOutputStream(itemUri);
            if (out == null) {
                if (itemUri != null) try { getContentResolver().delete(itemUri, null, null); } catch (Exception ignored) {}
                throw new IOException("Failed to open output stream for URI: " + itemUri);
            }
            in = new FileInputStream(sourceFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            values.clear();
            values.put(MediaStore.Audio.Media.IS_PENDING, 0);
            getContentResolver().update(itemUri, values, null, null);
            Log.i(TAG, "Successfully saved recording to MediaStore: " + itemUri);

        } catch (IOException | SecurityException e) {
            if (itemUri != null) {
                try { getContentResolver().delete(itemUri, null, null); }
                catch (Exception deleteEx) { Log.e(TAG, "Error deleting incomplete MediaStore entry", deleteEx); }
            }
            Log.e(TAG, "Error saving file to MediaStore", e);
            if (e instanceof SecurityException){ throw (SecurityException)e; }
            else { throw new IOException("Error saving file: " + e.getMessage(), e); }
        } finally {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
        }
    }

    // --- Activity Lifecycle Methods ---
    @Override
    protected void onPause() {
        super.onPause();
        // Pause playback when activity is paused
        if (mediaPlayer != null && isMediaPlayerPrepared() && mediaPlayer.isPlaying()) {
            pauseMediaPlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Consider releasing resources here if memory is critical,
        // but onDestroy is generally preferred for final cleanup.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "RecordResultActivity onDestroy called.");
        // Release resources to prevent leaks
        releaseMediaPlayer();
        stopUpdatingProgress();
        // Clean up the temporary mixed audio file created by FFmpeg
        deletePreviousFinalFile();
    }
}