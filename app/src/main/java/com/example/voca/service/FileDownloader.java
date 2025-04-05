package com.example.voca.service;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FileDownloader {
    private static final String TAG = "FileDownloader";

    public static void downloadExternalFile(Context context, String fileUrl, String fileName) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(fileUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        // Tạo file trong getExternalFilesDir(null)
                        File file = new File(context.getExternalFilesDir(null), fileName);
                        InputStream inputStream = body.byteStream();
                        FileOutputStream outputStream = new FileOutputStream(file);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();

                        Log.d(TAG, "Tải thành công: " + file.getAbsolutePath());
                    }
                } else {
                    Log.e(TAG, "Lỗi tải file: " + response.message());
                }
            } catch (IOException e) {
                Log.e(TAG, "Lỗi tải file", e);
            }
        }).start();
    }
}
