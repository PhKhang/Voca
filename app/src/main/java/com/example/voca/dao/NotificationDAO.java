package com.example.voca.dao;

import com.example.voca.api.NotificationApi;
import com.example.voca.dto.NotificationDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationDAO {
    private static final String BASE_URL = "https://voca-spda.onrender.com/";
    private NotificationApi notificationApi;

    public NotificationDAO() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        notificationApi = retrofit.create(NotificationApi.class);
    }

    public void getNotificationsByUserId(String userId, Callback<List<NotificationDTO>> callback) {
        Call<List<NotificationDTO>> call = notificationApi.getNotificationsByUserId(userId);
        call.enqueue(callback);
    }

    public void markNotificationAsRead(String id, Callback<NotificationDTO> callback) {
        Map<String, Boolean> body = new HashMap<>();
        body.put("is_read", true);
        Call<NotificationDTO> call = notificationApi.updateNotificationStatus(id, body);
        call.enqueue(callback);
    }

    public void markNotificationAsUnread(String id, Callback<NotificationDTO> callback) {
        Map<String, Boolean> body = new HashMap<>();
        body.put("is_read", false);
        Call<NotificationDTO> call = notificationApi.updateNotificationStatus(id, body);
        call.enqueue(callback);
    }

    public void deleteNotification(String id, Callback<Void> callback) {
        Call<Void> call = notificationApi.deleteNotification(id);
        call.enqueue(callback);
    }
}