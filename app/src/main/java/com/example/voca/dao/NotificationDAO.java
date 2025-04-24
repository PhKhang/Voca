package com.example.voca.dao;

import com.example.voca.api.NotificationApi;
import com.example.voca.dto.NotificationDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

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
        Call<NotificationDTO> call = notificationApi.markNotificationAsRead(id);
        call.enqueue(callback);
    }

    public void markNotificationAsUnread(String id, Callback<NotificationDTO> callback) {
        Call<NotificationDTO> call = notificationApi.markNotificationAsUnread(id);
        call.enqueue(callback);
    }

    public void deleteNotification(String id, Callback<Void> callback) {
        Call<Void> call = notificationApi.deleteNotification(id);
        call.enqueue(callback);
    }
}