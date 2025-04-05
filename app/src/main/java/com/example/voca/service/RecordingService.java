package com.example.voca.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.voca.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecordingService extends Service {
    private static final String CHANNEL_ID = "RecordingServiceChannel";
    private static final int SAMPLE_RATE = 44100;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AUDIO_FORMAT);

    private AudioRecord micRecorder;
    private FileOutputStream outputStream;
    private File audioFile;
    private boolean isRecording = false;
    private HandlerThread recordingThread;
    private Handler recordingHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, getNotification());
        startRecording();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Recording Service")
                .setContentText("Recording audio...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recording Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Nếu không có quyền, dừng lại và không ghi âm
            stopSelf();
            return;
        }
        try {
            audioFile = new File(getExternalFilesDir(null), "recorded_audio.wav");
            outputStream = new FileOutputStream(audioFile);

            micRecorder =
                    new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AUDIO_FORMAT, BUFFER_SIZE);

            isRecording = true;

            // Tạo HandlerThread để xử lý ghi âm trên luồng riêng biệt
            recordingThread = new HandlerThread("AudioRecordingThread");
            recordingThread.start();
            recordingHandler = new Handler(recordingThread.getLooper());

            recordingHandler.post(this::recordAudio);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recordAudio() {
        byte[] buffer = new byte[BUFFER_SIZE];

        micRecorder.startRecording();

        while (isRecording) {
            int read = micRecorder.read(buffer, 0, BUFFER_SIZE);

            try {
                outputStream.write(buffer, 0, read);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        stopAudio();
    }

    private void stopRecording() {
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.quitSafely();
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopAudio() {
        if (micRecorder != null) {
            micRecorder.stop();
            micRecorder.release();
            micRecorder = null;
        }
    }
}