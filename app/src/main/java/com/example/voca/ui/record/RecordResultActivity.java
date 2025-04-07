package com.example.voca.ui.record; // Thay đổi package nếu cần

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

import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState; // Import SessionState
import com.example.voca.R; // Thay đổi R nếu cần

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
    private TextView tvCurrentTime;
    private Button btnPlayPause, btnApplyEffects, btnConfirmAndCombine, saveButton; // btnConfirm đổi thành btnApplyEffects

    // File Paths
    private String originalRecordingPath; // Đường dẫn file M4A gốc
    private String backgroundMusicPath; // Đường dẫn nhạc nền (nếu cần ghép)
    private String processedRecordingPath; // Đường dẫn file M4A đã xử lý hiệu ứng

    // Media Player
    private MediaPlayer mediaPlayer;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    // FFmpeg filter parameters (lưu giá trị hiện tại)
    private float currentVolume = 1.0f; // Hệ số volume (1.0 = 100%)
    private float currentEchoDecay = 0.0f; // Decay (0.0 - 0.9)
    private int currentEchoDelay = 0; // Delay (ms)
    private int currentBassGain = 0; // Gain dB (-10 đến +10)
    private int currentTrebleGain = 0; // Gain dB (-10 đến +10)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.record_result_layout);

        // --- Get Intent Extras ---
        originalRecordingPath = getIntent().getStringExtra("recording_path");
        backgroundMusicPath = getIntent().getStringExtra("background_music_path"); // Vẫn lấy phòng khi cần ghép

        if (originalRecordingPath == null) {
            Log.e(TAG, "Missing original recording_path in Intent");
            Toast.makeText(this, "Lỗi: Thiếu đường dẫn file ghi âm gốc!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        File originalFile = new File(originalRecordingPath);
        if (!originalFile.exists()) {
            Log.e(TAG, "Original recording file does not exist: " + originalRecordingPath);
            Toast.makeText(this, "Lỗi: File ghi âm gốc không tồn tại!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Initialize UI Elements ---
        findViews();

        // --- Setup Listeners ---
        setupSeekBarListeners();
        setupButtonListeners();

        // --- Initialize Media Player ---
        mediaPlayer = new MediaPlayer();
        setupMediaPlayerListeners();

        // --- Set Initial State ---
        // Nút Play/Save/Combine bị vô hiệu hóa cho đến khi hiệu ứng được áp dụng
        btnPlayPause.setEnabled(false);
        saveButton.setEnabled(false);
        seekBarTime.setEnabled(false);
        // btnConfirmAndCombine.setEnabled(false); // Nếu có

        // Cập nhật giá trị ban đầu từ Seekbar mặc định
        updateEffectParametersFromSeekBars();
    }

    private void findViews() {
        seekBarTime = findViewById(R.id.seekBarTime);
        seekBarVolume = findViewById(R.id.seekBarVolume);
        seekBarEcho = findViewById(R.id.seekBarEcho);
        seekBarDelay = findViewById(R.id.seekBarDelay);
        seekBarBass = findViewById(R.id.seekBarBass);
        seekBarTreble = findViewById(R.id.seekBarTreble);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnApplyEffects = findViewById(R.id.btnApplyEffects);
        btnConfirmAndCombine = findViewById(R.id.btnConfirmAndCombine); // ID này có thể khác nếu bạn đổi tên
        saveButton = findViewById(R.id.save_button);
    }

    // Cập nhật biến lưu trữ tham số từ giá trị hiện tại của SeekBars
    private void updateEffectParametersFromSeekBars() {
        // Volume: SeekBar 0-200 -> Factor 0.0 - 2.0
        currentVolume = seekBarVolume.getProgress() / 100.0f;
        // Echo Decay: SeekBar 0-90 -> Factor 0.0 - 0.9
        currentEchoDecay = seekBarEcho.getProgress() / 100.0f;
        // Echo Delay: SeekBar 0-2000 -> ms 0 - 2000
        currentEchoDelay = seekBarDelay.getProgress();
        // Bass/Treble: SeekBar 0-20 -> dB -10 - +10 (Progress 10 là 0dB)
        currentBassGain = seekBarBass.getProgress() - 10;
        currentTrebleGain = seekBarTreble.getProgress() - 10;

        // Optional: Hiển thị giá trị cạnh SeekBar để người dùng thấy rõ hơn
        // Ví dụ: ((TextView)findViewById(R.id.tvVolumeValue)).setText(String.format("%.0f%%", currentVolume * 100));
    }

    private void setupSeekBarListeners() {
        // Listener chung cho các SeekBar hiệu ứng
        SeekBar.OnSeekBarChangeListener effectChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateEffectParametersFromSeekBars();
                    // Không cần làm gì thêm ở đây vì hiệu ứng chỉ áp dụng khi nhấn nút
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

        // Listener cho SeekBar thời gian (điều khiển MediaPlayer)
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int seekPosition = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekPosition = progress;
                    // Chỉ cập nhật text khi kéo, seek thực sự khi nhả tay
                    String current = formatTime(progress);
                    String total = formatTime(mediaPlayer.getDuration());
                    tvCurrentTime.setText(String.format("%s / %s", current, total));
                } else if (fromUser && mediaPlayer != null) {
                    // Cập nhật text ngay cả khi đang pause
                    seekPosition = progress;
                    String current = formatTime(progress);
                    String total = formatTime(mediaPlayer.getDuration());
                    tvCurrentTime.setText(String.format("%s / %s", current, total));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    progressHandler.removeCallbacks(progressRunnable); // Tạm dừng cập nhật tự động
                }
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekPosition);
                    if (mediaPlayer.isPlaying()) {
                        startUpdatingProgress(); // Bắt đầu lại cập nhật tự động
                    }
                    // Cập nhật lại text một lần nữa sau khi seek
                    String current = formatTime(seekPosition);
                    String total = formatTime(mediaPlayer.getDuration());
                    tvCurrentTime.setText(String.format("%s / %s", current, total));
                }
            }
        });
    }

    private void setupButtonListeners() {
        // Nút áp dụng hiệu ứng
        btnApplyEffects.setOnClickListener(v -> applyEffectsWithFFmpeg());

        // Nút Play/Pause
        btnPlayPause.setOnClickListener(v -> togglePlayback());

        // Nút Lưu
        saveButton.setOnClickListener(v -> {
            if (processedRecordingPath != null && new File(processedRecordingPath).exists()) {
                try {
                    saveToRecordings(processedRecordingPath);
                    Toast.makeText(this, "Đã lưu file đã xử lý", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e(TAG, "Error saving processed file", e);
                    Toast.makeText(this, "Lỗi khi lưu file", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Chưa có file đã xử lý để lưu", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Ghép file (ví dụ, bạn cần hoàn thiện logic này)
        btnConfirmAndCombine.setOnClickListener(v -> {
            if (processedRecordingPath == null || !new File(processedRecordingPath).exists()) {
                Toast.makeText(this, "Cần áp dụng hiệu ứng cho giọng hát trước khi ghép", Toast.LENGTH_SHORT).show();
                return;
            }
            if (backgroundMusicPath == null || !new File(backgroundMusicPath).exists()) {
                Toast.makeText(this, "Không tìm thấy file nhạc nền để ghép", Toast.LENGTH_SHORT).show();
                return;
            }
            // Gọi hàm ghép file (tương tự applyEffectsWithFFmpeg nhưng dùng filter amix)
            combineVoiceAndMusic(processedRecordingPath, backgroundMusicPath);
        });
    }

    private void setupMediaPlayerListeners() {
        mediaPlayer.setOnPreparedListener(mp -> {
            Log.d(TAG, "MediaPlayer prepared. Duration: " + mp.getDuration());
            seekBarTime.setMax(mp.getDuration());
            seekBarTime.setEnabled(true);
            btnPlayPause.setEnabled(true);
            saveButton.setEnabled(true); // Cho phép lưu khi đã có file để phát
            // Hiển thị tổng thời gian
            String total = formatTime(mp.getDuration());
            tvCurrentTime.setText(String.format("00:00 / %s", total));

            mp.start();
            btnPlayPause.setText("Pause");
            startUpdatingProgress();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "MediaPlayer completion.");
            btnPlayPause.setText("Play");
            seekBarTime.setProgress(0);
            tvCurrentTime.setText(String.format("00:00 / %s", formatTime(mp.getDuration())));
            stopUpdatingProgress();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);
            Toast.makeText(this, "Lỗi phát nhạc: " + what, Toast.LENGTH_SHORT).show();
            resetMediaPlayer();
            return true; // Đã xử lý lỗi
        });
    }

    // --- FFmpeg Execution ---

    private void applyEffectsWithFFmpeg() {
        if (originalRecordingPath == null) return;

        updateEffectParametersFromSeekBars(); // Lấy giá trị mới nhất

        // Tạo tên file output duy nhất trong thư mục cache hoặc filesDir
        String outputFileName = "processed_" + System.currentTimeMillis() + ".m4a"; // Giữ định dạng m4a (AAC)
        File outputFile = new File(getExternalFilesDir(null), outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();

        // Xây dựng chuỗi filter_complex
        StringBuilder filterGraph = new StringBuilder();
        String currentInput = "[0:a]"; // Luồng âm thanh đầu vào từ file 0

        // 1. Volume
        filterGraph.append(String.format(Locale.US, "%svolume=%.2f[vol];", currentInput, currentVolume));
        currentInput = "[vol]";

        // 2. Bass (nếu gain khác 0)
        if (Math.abs(currentBassGain) > 0.1) { // Chỉ áp dụng nếu có thay đổi đáng kể
            filterGraph.append(String.format(Locale.US, "%sbass=g=%d:f=100:w=0.5[bass];", currentInput, currentBassGain));
            currentInput = "[bass]";
        }

        // 3. Treble (nếu gain khác 0)
        if (Math.abs(currentTrebleGain) > 0.1) {
            filterGraph.append(String.format(Locale.US, "%streble=g=%d:f=3000:w=0.5[treble];", currentInput, currentTrebleGain));
            currentInput = "[treble]";
        }

        // 4. Echo (nếu delay > 0 và decay > 0)
        if (currentEchoDelay > 0 && currentEchoDecay > 0.01) {
            // aecho=input_gain:output_gain:delays:decays
            // Giữ input_gain=0.8, output_gain=0.6 (có thể điều chỉnh)
            filterGraph.append(String.format(Locale.US, "%saecho=0.8:0.6:%d:%.2f[echo];", currentInput, currentEchoDelay, currentEchoDecay));
            currentInput = "[echo]";
        }

        // Xóa dấu ; cuối cùng nếu có
        if (filterGraph.length() > 0 && filterGraph.charAt(filterGraph.length() - 1) == ';') {
            filterGraph.setLength(filterGraph.length() - 1);
        }

        // Nếu không có filter nào được thêm, filterGraph sẽ rỗng
        if (filterGraph.length() == 0) {
            // Nếu không có hiệu ứng nào, chỉ cần copy file hoặc không làm gì cả
            // Để đơn giản, ta vẫn chạy lệnh FFmpeg nhưng không có filter phức tạp
            // Hoặc bạn có thể xử lý trường hợp này riêng
            Toast.makeText(this, "Không có hiệu ứng nào được chọn.", Toast.LENGTH_SHORT).show();
            // Sao chép file gốc làm file đã xử lý
            copyFileForPlayback(originalRecordingPath);
            return; // Không cần chạy FFmpeg
        }

        // Lệnh FFmpeg hoàn chỉnh
        // -map "%s" : Ánh xạ output cuối cùng của filter graph vào file output
        // -c:a aac : Encode lại bằng AAC (cho M4A)
        // -b:a 192k : Bitrate (tùy chọn)
        String ffmpegCommand = String.format(Locale.US,
                "-y -i \"%s\" -filter_complex \"%s\" -map \"%s\" -c:a aac -b:a 192k \"%s\"",
                originalRecordingPath, filterGraph.toString(), currentInput, outputFilePath
        );

        Log.d(TAG, "Executing FFmpeg command: " + ffmpegCommand);

        // --- Thực thi FFmpeg ---
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang áp dụng hiệu ứng...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Vô hiệu hóa các nút trong khi xử lý
        setButtonsEnabled(false);

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            final SessionState state = session.getState();
            final ReturnCode returnCode = session.getReturnCode();

            runOnUiThread(() -> {
                progressDialog.dismiss();
                setButtonsEnabled(true); // Bật lại nút Apply

                if (ReturnCode.isSuccess(returnCode)) {
                    Log.d(TAG, "FFmpeg process completed successfully.");
                    Toast.makeText(RecordResultActivity.this, "Áp dụng hiệu ứng thành công!", Toast.LENGTH_SHORT).show();

                    // Lưu đường dẫn file đã xử lý
                    processedRecordingPath = outputFilePath;

                    // Kích hoạt các nút điều khiển playback và lưu
                    btnPlayPause.setEnabled(true);
                    saveButton.setEnabled(true);
                    seekBarTime.setEnabled(true);
                    // btnConfirmAndCombine.setEnabled(true); // Nếu có

                    // Chuẩn bị và phát file đã xử lý
                    prepareAndPlayMediaPlayer(processedRecordingPath);

                } else {
                    Log.e(TAG, String.format("FFmpeg process failed! State: %s, Return Code: %s", state, returnCode));
                    Log.e(TAG, "FFmpeg Logs: " + session.getAllLogsAsString());
                    Toast.makeText(RecordResultActivity.this, "Lỗi khi áp dụng hiệu ứng: " + returnCode, Toast.LENGTH_LONG).show();

                    // Giữ các nút playback/save bị vô hiệu hóa
                    processedRecordingPath = null; // Reset đường dẫn
                    btnPlayPause.setEnabled(false);
                    saveButton.setEnabled(false);
                    seekBarTime.setEnabled(false);
                    // btnConfirmAndCombine.setEnabled(false);
                }
            });
        });
    }

    // Hàm để sao chép file khi không có hiệu ứng nào
    private void copyFileForPlayback(String sourcePath) {
        String outputFileName = "processed_" + System.currentTimeMillis() + ".m4a";
        File outputFile = new File(getExternalFilesDir(null), outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();

        try (FileInputStream in = new FileInputStream(sourcePath);
             OutputStream out = new FileOutputStream(outputFilePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            processedRecordingPath = outputFilePath;
            Log.d(TAG, "Copied original file as processed file (no effects).");
            // Kích hoạt các nút điều khiển playback và lưu
            btnPlayPause.setEnabled(true);
            saveButton.setEnabled(true);
            seekBarTime.setEnabled(true);
            // btnConfirmAndCombine.setEnabled(true);

            // Chuẩn bị và phát file đã xử lý
            prepareAndPlayMediaPlayer(processedRecordingPath);

        } catch (IOException e) {
            Log.e(TAG, "Error copying file when no effects applied", e);
            Toast.makeText(this, "Lỗi khi sao chép file gốc", Toast.LENGTH_SHORT).show();
            processedRecordingPath = null;
            btnPlayPause.setEnabled(false);
            saveButton.setEnabled(false);
            seekBarTime.setEnabled(false);
            // btnConfirmAndCombine.setEnabled(false);
        }
    }


    // Hàm ghép file (ví dụ cơ bản)
    private void combineVoiceAndMusic(String voicePath, String musicPath) {
        String outputFileName = "combined_" + System.currentTimeMillis() + ".mp3"; // Output MP3
        File outputFile = new File(getExternalFilesDir(null), outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();

        // Lệnh amix cơ bản, dừng khi file ngắn nhất kết thúc
        String ffmpegCommand = String.format(Locale.US,
                "-y -i \"%s\" -i \"%s\" -filter_complex \"[0:a][1:a]amix=inputs=2:duration=shortest[aout]\" -map \"[aout]\" -c:a libmp3lame -q:a 2 \"%s\"",
                voicePath, musicPath, outputFilePath
        );

        Log.d(TAG, "Executing FFmpeg combine command: " + ffmpegCommand);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang ghép file...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        setButtonsEnabled(false);

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            final SessionState state = session.getState();
            final ReturnCode returnCode = session.getReturnCode();
            runOnUiThread(() -> {
                progressDialog.dismiss();
                setButtonsEnabled(true); // Bật lại các nút chính

                if (ReturnCode.isSuccess(returnCode)) {
                    Log.d(TAG, "FFmpeg combine completed successfully.");
                    Toast.makeText(RecordResultActivity.this, "Ghép file thành công!", Toast.LENGTH_SHORT).show();
                    // Có thể phát file vừa ghép
                    prepareAndPlayMediaPlayer(outputFilePath);
                    // Hoặc chỉ lưu đường dẫn và cho phép lưu
                    // processedRecordingPath = outputFilePath; // Cập nhật để nút Save lưu file đã ghép
                    // saveButton.setEnabled(true);
                } else {
                    Log.e(TAG, String.format("FFmpeg combine failed! State: %s, Return Code: %s", state, returnCode));
                    Log.e(TAG, "FFmpeg Logs: " + session.getAllLogsAsString());
                    Toast.makeText(RecordResultActivity.this, "Lỗi khi ghép file: " + returnCode, Toast.LENGTH_LONG).show();
                }
            });
        });
    }


    // --- Media Player Controls ---

    private void togglePlayback() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.isPlaying()) {
            pauseMediaPlayer();
        } else {
            if (processedRecordingPath != null) {
                // Nếu đang pause thì resume, nếu chưa chuẩn bị thì chuẩn bị
                if (mediaPlayer.getCurrentPosition() > 0 && !mediaPlayer.isPlaying()) { // Check if paused
                    mediaPlayer.start();
                    btnPlayPause.setText("Pause");
                    startUpdatingProgress();
                } else { // Not prepared or stopped, prepare again
                    prepareAndPlayMediaPlayer(processedRecordingPath);
                }
            } else {
                Toast.makeText(this, "Cần áp dụng hiệu ứng trước khi phát", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void prepareAndPlayMediaPlayer(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "prepareAndPlayMediaPlayer: filePath is null or empty");
            return;
        }
        File file = new File(filePath);
        if(!file.exists()){
            Log.e(TAG, "prepareAndPlayMediaPlayer: File does not exist: " + filePath);
            Toast.makeText(this, "File không tồn tại!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Preparing MediaPlayer for: " + filePath);
        resetMediaPlayer(); // Reset trước khi chuẩn bị file mới
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync(); // Chuẩn bị bất đồng bộ
            btnPlayPause.setText("Loading..."); // Hiển thị trạng thái chờ
            btnPlayPause.setEnabled(false); // Vô hiệu hóa tạm thời
            seekBarTime.setEnabled(false);
        } catch (IOException e) {
            Log.e(TAG, "IOException setting data source", e);
            Toast.makeText(this, "Lỗi tải file âm thanh", Toast.LENGTH_SHORT).show();
            resetMediaPlayer();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException setting data source", e);
            Toast.makeText(this, "Lỗi trạng thái trình phát", Toast.LENGTH_SHORT).show();
            resetMediaPlayer();
        }
    }

    private void pauseMediaPlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setText("Play");
            stopUpdatingProgress();
        }
    }

    private void resetMediaPlayer() {
        if (mediaPlayer != null) {
            stopUpdatingProgress();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset(); // Reset về trạng thái Idle
            Log.d(TAG, "MediaPlayer reset.");
        }
        // Reset UI liên quan đến playback
        seekBarTime.setProgress(0);
        seekBarTime.setEnabled(false);
        tvCurrentTime.setText("00:00 / 00:00");
        btnPlayPause.setText("Play");
        btnPlayPause.setEnabled(false); // Vô hiệu hóa cho đến khi chuẩn bị xong
        saveButton.setEnabled(false); // Vô hiệu hóa cho đến khi chuẩn bị xong
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            stopUpdatingProgress();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer released.");
        }
    }

    // --- Progress Update ---

    private void startUpdatingProgress() {
        if (mediaPlayer == null) return;
        stopUpdatingProgress(); // Đảm bảo không có runnable cũ

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBarTime.setProgress(currentPosition);
                    String current = formatTime(currentPosition);
                    String total = formatTime(mediaPlayer.getDuration());
                    tvCurrentTime.setText(String.format("%s / %s", current, total));

                    progressHandler.postDelayed(this, 500); // Cập nhật mỗi 500ms
                }
            }
        };
        progressHandler.post(progressRunnable); // Bắt đầu ngay lập tức
    }

    private void stopUpdatingProgress() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    // --- Utility Functions ---

    private String formatTime(int milliseconds) {
        if (milliseconds < 0) milliseconds = 0;
        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    // Helper để bật/tắt các nút chính
    private void setButtonsEnabled(boolean enabled) {
        btnApplyEffects.setEnabled(enabled);
        // Chỉ bật các nút khác nếu đã có file xử lý và không đang xử lý
        boolean canEnableOthers = enabled && (processedRecordingPath != null && new File(processedRecordingPath).exists());
        btnPlayPause.setEnabled(canEnableOthers);
        saveButton.setEnabled(canEnableOthers);
        seekBarTime.setEnabled(canEnableOthers);
        // btnConfirmAndCombine.setEnabled(canEnableOthers && backgroundMusicPath != null); // Chỉ bật nếu có nhạc nền
    }

    // Lưu file vào Recordings
    private void saveToRecordings(String sourceFilePath) throws IOException {
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            throw new IOException("Source file not found: " + sourceFilePath);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String originalSongName = getIntent().getStringExtra("song_name"); // Lấy tên bài hát gốc nếu có
        if (originalSongName == null || originalSongName.isEmpty()) {
            originalSongName = "ProcessedRecording"; // Tên mặc định
        }
        originalSongName = originalSongName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String uniqueFileName = originalSongName + "_Effects_" + timestamp + ".m4a"; // Giữ .m4a

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, uniqueFileName);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4"); // MIME type cho M4A
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);

        Uri collectionUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RECORDINGS);
            }
            collectionUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            File recordingsDir = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS);
            }
            if (!recordingsDir.exists()) recordingsDir.mkdirs();
            File targetFile = new File(recordingsDir, uniqueFileName);
            values.put(MediaStore.Audio.Media.DATA, targetFile.getAbsolutePath());
            collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        Uri itemUri = getContentResolver().insert(collectionUri, values);

        if (itemUri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(itemUri);
                 FileInputStream in = new FileInputStream(sourceFile)) {
                if (out == null) throw new IOException("Failed to open output stream for URI: " + itemUri);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            } catch (IOException e) {
                getContentResolver().delete(itemUri, null, null); // Xóa nếu lỗi
                throw e;
            } finally {
                values.clear();
                values.put(MediaStore.Audio.Media.IS_PENDING, 0);
                getContentResolver().update(itemUri, values, null, null);
            }
        } else {
            throw new IOException("Failed to create MediaStore entry.");
        }
    }

    // --- Lifecycle ---

    @Override
    protected void onPause() {
        super.onPause();
        pauseMediaPlayer(); // Tạm dừng khi activity không hiển thị
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer(); // Luôn giải phóng MediaPlayer
        stopUpdatingProgress(); // Dừng handler
        // Optional: Xóa file processedRecordingPath tạm thời nếu không muốn giữ lại
        // if (processedRecordingPath != null) {
        //     File tempFile = new File(processedRecordingPath);
        //     if (tempFile.exists() && isExternalFilesDir(tempFile.getParentFile())) { // Chỉ xóa nếu nó nằm trong thư mục của app
        //         tempFile.delete();
        //         Log.d(TAG, "Deleted temporary processed file: " + processedRecordingPath);
        //     }
        // }
    }
    // Helper kiểm tra xem thư mục có phải là thư mục của ứng dụng không
    // private boolean isExternalFilesDir(File dir) {
    //     return dir != null && dir.equals(getExternalFilesDir(null));
    // }
}