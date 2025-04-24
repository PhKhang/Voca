package com.example.voca.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class FetchVideoTitleTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "FetchVideoTitleTask";
    private final Context context;
    private final EditText titleEditText;
    private final String SERVER_URL = "https://voca-spda.onrender.com/fetchYouTubeTitle";

    public FetchVideoTitleTask(Context context, EditText titleEditText) {
        this.context = context;
        this.titleEditText = titleEditText;
    }

    @Override
    protected String doInBackground(String... videoIds) {
        String videoId = videoIds[0];
        String title = null;
        try {
            URL url = new URL(SERVER_URL + "?videoId=" + videoId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.lines().collect(Collectors.joining());
                in.close();
                JSONObject jsonResponse = new JSONObject(response);
                title = jsonResponse.optString("title", null);
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching video title", e);
        }
        return title;
    }

    @Override
    protected void onPostExecute(String title) {
        if (title != null) {
            titleEditText.setText(title);
        } else {
            titleEditText.setText("");
            Toast.makeText(context, "Không thể lấy tiêu đề video", Toast.LENGTH_SHORT).show();
        }
    }
}