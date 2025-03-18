package com.example.voca.service;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploader {
    Context context;
    public void run(Context context, Uri uri) {
        this.context = context;
        File file;
        try {
            file = getFileFromUri(uri);
        } catch (IOException e) {
            System.out.println("Cannot get file from uri");
            e.printStackTrace();
            return;
        }
        OkHttpClient client = new OkHttpClient();
        if (file == null)
            System.out.println("file is null");

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/mpeg"));
        System.out.println("fileBody created");

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)  // File field
                .build();
        System.out.println("Multipart created");

        Request request = new Request.Builder()
                .url("http://10.0.2.2:3000/upload")
                .post(requestBody)
                .build();

        System.out.println("Request starting...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try (Response response = client.newCall(request).execute()) {
                System.out.println("Response: " + response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    e.printStackTrace();
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    System.out.println("Response: " + response.body().string());
//                }
//            });
    }

    File getFileFromUri(Uri uri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        String fileName = null;

//            Getting filename
        try {
            Cursor cursor = null;
            cursor = contentResolver.query(uri, null, null, null, null);
            fileName = null;
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1)
                    fileName = cursor.getString(index);
            }

            if (fileName == null) {
                fileName = uri.getPath();
                int cut = fileName.lastIndexOf('/');
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1);
                }
            }
        } catch (Exception e) {
            System.out.println("Cannot resolve cursor from uri");
            e.printStackTrace();
        }
        System.out.println("Real file name is: " + fileName);

//            Get file byte by byte
        File file = new File(context.getCacheDir(), fileName);
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return file;
    }
}
