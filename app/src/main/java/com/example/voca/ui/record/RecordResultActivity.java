package com.example.voca.ui.record; // Ensure this matches your package

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable; // Import if needed
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Import if using resources

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.example.voca.R; // Ensure this matches your R file

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordResultActivity extends AppCompatActivity {
    private static final String TAG = "RecordResultActivity_FFmpeg";

    // UI Elements
    private SeekBar seekBarTime, seekBarVolume, seekBarEcho, seekBarDelay, seekBarBass, seekBarTreble;
    private TextView tvCurrentTime, tvSongNameResult; // Added TextView for song name
    private Button btnPlayPause, btnPreviewEffects, saveButton; // Renamed btnApplyEffects, Removed btnConfirmAndCombine

    // File Paths
    private String originalRecordingPath; // Path to the user's raw voice recording (M4A)
    private String backgroundMusicPath; // Path to the background music (MP3/M4A etc.)
    private String processedAndMixedPath; // Path to the temporary file containing voice+effects+music (MP3)
    private String songName; // To display and use in saved filename

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.record_result_layout);

        // --- Get Intent Extras ---
        originalRecordingPath = getIntent().getStringExtra("recording_path");
        backgroundMusicPath = getIntent().getStringExtra("background_music_path");
        songName = getIntent().getStringExtra("song_name"); // Get song name

        // --- Basic Validation ---
        if (originalRecordingPath == null || backgroundMusicPath == null || songName == null) {
            Log.e(TAG, "Missing required paths or song name in Intent");
            Toast.makeText(this, "Lỗi: Thiếu thông tin bài hát hoặc file âm thanh!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        File originalFile = new File(originalRecordingPath);
        File musicFile = new File(backgroundMusicPath);
        if (!originalFile.exists()) {
            Log.e(TAG, "Original recording file does not exist: " + originalRecordingPath);
            Toast.makeText(this, "Lỗi: File ghi âm gốc không tồn tại!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!musicFile.exists()) {
            Log.e(TAG, "Background music file does not exist: " + backgroundMusicPath);
            // Allow proceeding without music? Or force exit? Let's allow preview without music for now.
            // Toast.makeText(this, "Cảnh báo: File nhạc nền không tồn tại!", Toast.LENGTH_SHORT).show();
            // Forcing exit if music is essential:
            Toast.makeText(this, "Lỗi: File nhạc nền không tồn tại!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        // --- Initialize UI Elements ---
        findViews();
        tvSongNameResult.setText(songName); // Display song name

        // --- Setup Listeners ---
        setupSeekBarListeners();
        setupButtonListeners();

        // --- Initialize Media Player ---
        mediaPlayer = new MediaPlayer();
        setupMediaPlayerListeners();

        // --- Set Initial State ---
        btnPlayPause.setEnabled(false);
        saveButton.setEnabled(false);
        seekBarTime.setEnabled(false);
        // btnPreviewEffects is enabled by default

        updateEffectParametersFromSeekBars(); // Set initial values
    }

    private void findViews() {
        seekBarTime = findViewById(R.id.seekBarTime);
        seekBarVolume = findViewById(R.id.seekBarVolume);
        seekBarEcho = findViewById(R.id.seekBarEcho);
        seekBarDelay = findViewById(R.id.seekBarDelay);
        seekBarBass = findViewById(R.id.seekBarBass);
        seekBarTreble = findViewById(R.id.seekBarTreble);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvSongNameResult = findViewById(R.id.tvSongNameResult); // Find the TextView
        btnPlayPause = findViewById(R.id.btnPlayPause);
        // **IMPORTANT:** Make sure the button ID in your layout matches R.id.btnPreviewEffects
        btnPreviewEffects = findViewById(R.id.btnApplyEffects); // Assuming ID is still btnApplyEffects
        saveButton = findViewById(R.id.save_button);

        // Remove or comment out the findView for the removed button
        // btnConfirmAndCombine = findViewById(R.id.btnConfirmAndCombine);
    }

    private void updateEffectParametersFromSeekBars() {
        currentVolume = seekBarVolume.getProgress() / 100.0f;
        currentEchoDecay = seekBarEcho.getProgress() / 100.0f;
        currentEchoDelay = seekBarDelay.getProgress();
        currentBassGain = seekBarBass.getProgress() - 10;
        currentTrebleGain = seekBarTreble.getProgress() - 10;
    }

    private void setupSeekBarListeners() {
        SeekBar.OnSeekBarChangeListener effectChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateEffectParametersFromSeekBars();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekBarVolume.setOnSeekBarChangeListener(effectChangeListener);
        seekBarEcho.setOnSeekBarChangeListener(effectChangeListener);
        seekBarDelay.setOnSeekBarChangeListener(effectChangeListener);
        seekBarBass.setOnSeekBarChangeListener(effectChangeListener);
        seekBarTreble.setOnSeekBarChangeListener(effectChangeListener);

        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int seekPosition = 0;
            boolean wasPlaying = false; // Track if playing before seeking

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekPosition = progress;
                    // Update time display immediately while scrubbing
                    String current = formatTime(progress);
                    String total = (mediaPlayer != null && mediaPlayer.getDuration() > 0)
                            ? formatTime(mediaPlayer.getDuration()) : "00:00";
                    tvCurrentTime.setText(String.format("%s / %s", current, total));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    wasPlaying = mediaPlayer.isPlaying(); // Store current playing state
                    if (wasPlaying) {
                        mediaPlayer.pause(); // Pause while seeking if playing
                        stopUpdatingProgress(); // Stop auto-updates
                        btnPlayPause.setText("Play"); // Update button text
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.getDuration() > 0) { // Check if duration is valid
                    if(seekPosition >= mediaPlayer.getDuration()){
                        seekPosition = mediaPlayer.getDuration() - 100; // Seek slightly before end
                        if(seekPosition < 0) seekPosition = 0;
                    }
                    mediaPlayer.seekTo(seekPosition);
                    // Update time display after seek
                    String current = formatTime(seekPosition);
                    String total = formatTime(mediaPlayer.getDuration());
                    tvCurrentTime.setText(String.format("%s / %s", current, total));

                    if (wasPlaying) {
                        mediaPlayer.start(); // Resume playing if it was playing before
                        startUpdatingProgress(); // Resume auto-updates
                        btnPlayPause.setText("Pause"); // Update button text
                    }
                }
            }
        });
    }

    private void setupButtonListeners() {
        // Button to apply effects AND mix with background music for preview
        btnPreviewEffects.setOnClickListener(v -> applyEffectsAndMixForPreview());

        // Play/Pause button for the preview file
        btnPlayPause.setOnClickListener(v -> togglePlayback());

        // Save the generated preview file
        saveButton.setOnClickListener(v -> {
            if (processedAndMixedPath != null && new File(processedAndMixedPath).exists()) {
                try {
                    saveToRecordings(processedAndMixedPath); // Save the mixed file
                    Toast.makeText(this, "Đã lưu bản thu", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e(TAG, "Error saving processed file", e);
                    Toast.makeText(this, "Lỗi khi lưu file", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Chưa tạo bản xem trước để lưu", Toast.LENGTH_SHORT).show();
            }
        });

        // Remove listener for btnConfirmAndCombine
    }

    private void setupMediaPlayerListeners() {
        mediaPlayer.setOnPreparedListener(mp -> {
            Log.d(TAG, "MediaPlayer prepared. Duration: " + mp.getDuration());
            if (mp.getDuration() <= 0) {
                Log.w(TAG, "MediaPlayer prepared with invalid duration (0 or less). Resetting.");
                Toast.makeText(this, "Lỗi: File âm thanh không hợp lệ.", Toast.LENGTH_SHORT).show();
                resetMediaPlayer();
                setPreviewControlsEnabled(false); // Disable controls
                return;
            }
            seekBarTime.setMax(mp.getDuration());
            setPreviewControlsEnabled(true); // Enable controls now

            String total = formatTime(mp.getDuration());
            tvCurrentTime.setText(String.format("00:00 / %s", total));

            mp.start();
            btnPlayPause.setText("Pause");
            startUpdatingProgress();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "MediaPlayer completion.");
            if (mp != null && mp.getDuration() > 0) { // Check duration before using it
                btnPlayPause.setText("Play");
                seekBarTime.setProgress(seekBarTime.getMax()); // Go to end
                tvCurrentTime.setText(String.format("%s / %s", formatTime(mp.getDuration()), formatTime(mp.getDuration())));
            } else {
                // Handle case where duration might be invalid on completion
                btnPlayPause.setText("Play");
                seekBarTime.setProgress(0);
                tvCurrentTime.setText("00:00 / 00:00");
            }
            stopUpdatingProgress();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);
            Toast.makeText(this, "Lỗi phát nhạc: " + what, Toast.LENGTH_SHORT).show();
            resetMediaPlayer();
            setPreviewControlsEnabled(false); // Disable controls on error
            return true;
        });
    }

    // --- FFmpeg Execution ---

    private void applyEffectsAndMixForPreview() {
        if (originalRecordingPath == null || backgroundMusicPath == null) {
            Toast.makeText(this, "Thiếu file ghi âm hoặc nhạc nền", Toast.LENGTH_SHORT).show();
            return;
        }
        File musicFile = new File(backgroundMusicPath);
        if (!musicFile.exists()) {
            Toast.makeText(this, "File nhạc nền không tồn tại", Toast.LENGTH_SHORT).show();
            return; // Stop if music is missing
        }

        updateEffectParametersFromSeekBars(); // Get latest effect values

        // Generate a unique filename for the temporary preview file (MP3 is often good for mixing)
        String outputFileName = "preview_" + System.currentTimeMillis() + ".mp3";
        File outputFile = new File(getExternalFilesDir(null), outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();

        // Build the complex filter graph
        StringBuilder filterGraph = new StringBuilder();
        String voiceInput = "[0:a]"; // Input stream from the first file (voice)
        String musicInput = "[1:a]"; // Input stream from the second file (music)
        String lastVoiceFilterOutput = voiceInput; // Track the output of the last voice effect

        // 1. Apply Voice Effects
        // Volume
        filterGraph.append(String.format(Locale.US, "%svolume=%.2f[vol];", lastVoiceFilterOutput, currentVolume));
        lastVoiceFilterOutput = "[vol]";

        // Bass
        if (Math.abs(currentBassGain) > 0.1) {
            filterGraph.append(String.format(Locale.US, "%sbass=g=%d:f=100:w=0.5[bass];", lastVoiceFilterOutput, currentBassGain));
            lastVoiceFilterOutput = "[bass]";
        }

        // Treble
        if (Math.abs(currentTrebleGain) > 0.1) {
            filterGraph.append(String.format(Locale.US, "%streble=g=%d:f=3000:w=0.5[treble];", lastVoiceFilterOutput, currentTrebleGain));
            lastVoiceFilterOutput = "[treble]";
        }

        // Echo
        if (currentEchoDelay > 0 && currentEchoDecay > 0.01) {
            filterGraph.append(String.format(Locale.US, "%saecho=0.8:0.6:%d:%.2f[echo];", lastVoiceFilterOutput, currentEchoDelay, currentEchoDecay));
            lastVoiceFilterOutput = "[echo]";
        }

        // 2. Mix Effected Voice with Background Music
        // Use amix. duration=first makes the output length match the first input (effected voice).
        // dropout_transition helps smooth the end if music is longer.
        filterGraph.append(String.format(Locale.US, "%s%samix=inputs=2:duration=first:dropout_transition=3[aout]",
                lastVoiceFilterOutput, musicInput));

        // Construct the full FFmpeg command
        // -i voice_rec.m4a -i music.mp3 -filter_complex "..." -map "[aout]" -c:a libmp3lame -q:a 2 output.mp3
        String ffmpegCommand = String.format(Locale.US,
                "-y -i \"%s\" -i \"%s\" -filter_complex \"%s\" -map \"[aout]\" -c:a libmp3lame -q:a 2 \"%s\"",
                originalRecordingPath,
                backgroundMusicPath,
                filterGraph.toString(),
                outputFilePath
        );

        Log.d(TAG, "Executing FFmpeg command: " + ffmpegCommand);

        // --- Execute FFmpeg Async ---
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lý và trộn âm thanh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Disable buttons during processing
        setExecutionInProgress(true);

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            final SessionState state = session.getState();
            final ReturnCode returnCode = session.getReturnCode();

            runOnUiThread(() -> {
                progressDialog.dismiss();
                setExecutionInProgress(false); // Re-enable buttons

                if (ReturnCode.isSuccess(returnCode)) {
                    Log.d(TAG, "FFmpeg process completed successfully.");
                    File resultFile = new File(outputFilePath);
                    if (resultFile.exists() && resultFile.length() > 0) {
                        Toast.makeText(RecordResultActivity.this, "Xem trước hiệu ứng và nhạc nền", Toast.LENGTH_SHORT).show();

                        // Delete previous preview file if it exists
                        deletePreviousPreviewFile();

                        // Store the path to the NEW preview file
                        processedAndMixedPath = outputFilePath;

                        // Prepare and play the NEW mixed file
                        prepareAndPlayMediaPlayer(processedAndMixedPath);
                        // Controls (Play/Pause, Save, Seek) will be enabled in onPreparedListener

                    } else {
                        Log.e(TAG, "FFmpeg succeeded but output file is missing or empty: " + outputFilePath);
                        Toast.makeText(RecordResultActivity.this, "Lỗi: Không tạo được file xem trước", Toast.LENGTH_LONG).show();
                        processedAndMixedPath = null;
                        setPreviewControlsEnabled(false);
                    }

                } else {
                    Log.e(TAG, String.format("FFmpeg process failed! State: %s, Return Code: %s", state, returnCode));
                    Log.e(TAG, "FFmpeg Logs: " + session.getAllLogsAsString());
                    Toast.makeText(RecordResultActivity.this, "Lỗi khi xử lý âm thanh: " + returnCode, Toast.LENGTH_LONG).show();
                    processedAndMixedPath = null; // Reset path on failure
                    resetMediaPlayer(); // Ensure player is reset
                    setPreviewControlsEnabled(false); // Disable playback controls
                }
            });
        });
    }

    private void deletePreviousPreviewFile() {
        if (processedAndMixedPath != null) {
            File previousFile = new File(processedAndMixedPath);
            if (previousFile.exists() && previousFile.isFile()) {
                if (previousFile.delete()) {
                    Log.d(TAG, "Deleted previous preview file: " + processedAndMixedPath);
                } else {
                    Log.w(TAG, "Failed to delete previous preview file: " + processedAndMixedPath);
                }
            }
            processedAndMixedPath = null; // Clear the path regardless
        }
    }

    // --- Media Player Controls ---

    private void togglePlayback() {
        if (mediaPlayer == null || processedAndMixedPath == null) {
            Toast.makeText(this, "Chưa có bản xem trước để phát", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                pauseMediaPlayer();
            } else {
                // Check if player needs preparing or just resuming
                // Get duration check prevents errors if reset occurred
                if (mediaPlayer.getDuration() > 0 && !mediaPlayer.isPlaying()){ // Was paused or stopped but prepared
                    mediaPlayer.start();
                    btnPlayPause.setText("Pause");
                    startUpdatingProgress();
                } else { // Needs preparation
                    prepareAndPlayMediaPlayer(processedAndMixedPath); // Prepare and auto-plays on prepared
                }
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer state error during togglePlayback", e);
            Toast.makeText(this, "Lỗi trình phát, đang thử lại...", Toast.LENGTH_SHORT).show();
            prepareAndPlayMediaPlayer(processedAndMixedPath); // Attempt to recover by preparing again
        }
    }


    private void prepareAndPlayMediaPlayer(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "prepareAndPlayMediaPlayer: filePath is null or empty");
            return;
        }
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            Log.e(TAG, "prepareAndPlayMediaPlayer: File does not exist or is empty: " + filePath);
            Toast.makeText(this, "File xem trước không hợp lệ!", Toast.LENGTH_SHORT).show();
            setPreviewControlsEnabled(false);
            return;
        }

        Log.d(TAG, "Preparing MediaPlayer for: " + filePath);
        resetMediaPlayer(); // Reset before preparing new file
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync(); // Prepare asynchronously
            btnPlayPause.setText("Loading...");
            setPreviewControlsEnabled(false); // Disable controls until prepared
        } catch (IOException e) {
            Log.e(TAG, "IOException setting data source", e);
            Toast.makeText(this, "Lỗi tải file âm thanh", Toast.LENGTH_SHORT).show();
            resetMediaPlayer();
            setPreviewControlsEnabled(false);
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException setting data source", e);
            Toast.makeText(this, "Lỗi trạng thái trình phát", Toast.LENGTH_SHORT).show();
            resetMediaPlayer();
            setPreviewControlsEnabled(false);
        }
    }

    private void pauseMediaPlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                btnPlayPause.setText("Play");
                stopUpdatingProgress();
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException during pause", e);
                resetMediaPlayer(); // Reset if state is invalid
                setPreviewControlsEnabled(false);
            }
        }
    }

    private void resetMediaPlayer() {
        if (mediaPlayer != null) {
            stopUpdatingProgress();
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset(); // Reset to Idle state
                Log.d(TAG, "MediaPlayer reset.");
            } catch (IllegalStateException e){
                Log.e(TAG,"IllegalStateException during reset, recreating MediaPlayer.", e);
                mediaPlayer.release();
                mediaPlayer = new MediaPlayer(); // Create a new instance
                setupMediaPlayerListeners(); // Re-attach listeners
            }
        }
        // Reset UI related to playback
        seekBarTime.setProgress(0);
        tvCurrentTime.setText("00:00 / 00:00");
        btnPlayPause.setText("Play");
        // Controls are typically disabled here and re-enabled on prepare/success
        setPreviewControlsEnabled(false);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            stopUpdatingProgress();
            try {
                mediaPlayer.release(); // Release resources
            } catch (Exception e) {
                Log.e(TAG,"Exception during MediaPlayer release", e);
            }
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer released.");
        }
    }

    // --- Progress Update ---

    private void startUpdatingProgress() {
        if (mediaPlayer == null || progressHandler == null) return;
        stopUpdatingProgress(); // Ensure no duplicates

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int duration = mediaPlayer.getDuration();
                        // Basic validation for duration
                        if(duration <= 0) {
                            stopUpdatingProgress();
                            return;
                        }
                        if (currentPosition > duration) {
                            currentPosition = duration; // Cap position at duration
                        }
                        seekBarTime.setProgress(currentPosition);
                        String current = formatTime(currentPosition);
                        String total = formatTime(duration);
                        tvCurrentTime.setText(String.format("%s / %s", current, total));

                        progressHandler.postDelayed(this, 500); // Update every 500ms
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG,"Error updating progress: " + e.getMessage());
                    stopUpdatingProgress(); // Stop updates on error
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

    // Helper to enable/disable buttons during FFmpeg processing
    private void setExecutionInProgress(boolean inProgress) {
        btnPreviewEffects.setEnabled(!inProgress);
        // Disable playback controls while processing
        if(inProgress){
            setPreviewControlsEnabled(false);
            if(mediaPlayer != null && mediaPlayer.isPlaying()){
                pauseMediaPlayer(); // Pause if playing when processing starts
            }
        }
        // Note: Playback controls are re-enabled selectively upon successful processing
    }

    // Helper to enable/disable PLAYBACK controls (Play/Pause, Seek, Save)
    private void setPreviewControlsEnabled(boolean enabled) {
        // Only enable if a valid preview file exists
        boolean canEnable = enabled && (processedAndMixedPath != null && new File(processedAndMixedPath).exists());

        btnPlayPause.setEnabled(canEnable);
        saveButton.setEnabled(canEnable);
        seekBarTime.setEnabled(canEnable);
        // Update button text based on state if enabling
        if(canEnable && mediaPlayer != null){
            btnPlayPause.setText(mediaPlayer.isPlaying() ? "Pause" : "Play");
        } else if (!canEnable) {
            btnPlayPause.setText("Play"); // Reset text if disabling
        }
    }


    // Save the MIXED file to Recordings
    private void saveToRecordings(String sourceFilePath) throws IOException {
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            throw new IOException("Source file not found: " + sourceFilePath);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        // Use the songName passed via intent for a more descriptive filename
        String baseName = (songName != null && !songName.isEmpty()) ? songName : "VocaRecording";
        baseName = baseName.replaceAll("[^a-zA-Z0-9.-]", "_"); // Sanitize filename
        // Output was MP3, so save as MP3
        String uniqueFileName = baseName + "_Mix_" + timestamp + ".mp3";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, uniqueFileName);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg"); // MIME type for MP3
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);

        Uri collectionUri;
        String relativePath = null; // Standard recordings folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            relativePath = Environment.DIRECTORY_RECORDINGS;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath);
            collectionUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            File recordingsDir = Environment.getExternalStoragePublicDirectory(relativePath);
            if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
                Log.e(TAG, "Failed to create Recordings directory");
                throw new IOException("Failed to create Recordings directory");
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
                throw new IOException("Failed to open output stream for URI: " + itemUri);
            }
            in = new FileInputStream(sourceFile);

            byte[] buffer = new byte[4096]; // 4KB buffer
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush(); // Ensure all data is written

            // File write successful, clear IS_PENDING flag
            values.clear();
            values.put(MediaStore.Audio.Media.IS_PENDING, 0);
            getContentResolver().update(itemUri, values, null, null);

            Log.d(TAG,"Successfully saved recording to: " + itemUri);

        } catch (IOException e) {
            // An error occurred, attempt to delete the incomplete MediaStore entry
            if (itemUri != null) {
                try {
                    getContentResolver().delete(itemUri, null, null);
                    Log.w(TAG,"Deleted incomplete MediaStore entry due to error: " + itemUri);
                } catch (Exception deleteEx) {
                    Log.e(TAG, "Error deleting incomplete MediaStore entry: " + itemUri, deleteEx);
                }
            }
            throw e; // Re-throw the original exception
        } finally {
            // Close streams safely
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
        }
    }


    // --- Lifecycle ---

    @Override
    protected void onPause() {
        super.onPause();
        // Pause playback when the activity is paused, but don't release yet
        // because the user might come back immediately.
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            pauseMediaPlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // If the app is truly stopped (not just paused), consider releasing resources.
        // However, releasing here might cause issues if the user quickly switches back.
        // onDestroy is a safer place for full release.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer(); // Always release MediaPlayer
        stopUpdatingProgress(); // Stop handler callbacks
        deletePreviousPreviewFile(); // Clean up the temporary preview file
        Log.d(TAG,"RecordResultActivity destroyed.");
    }
}