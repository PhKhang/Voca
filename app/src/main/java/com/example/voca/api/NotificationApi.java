package com.example.voca.api;

import com.example.voca.dto.NotificationDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;

import java.util.List;
import java.util.Map;

public interface NotificationApi {
    @GET("notifications/{userId}")
    Call<List<NotificationDTO>> getNotificationsByUserId(@Path("userId") String userId);

    @PUT("notifications/{id}/status")
    Call<NotificationDTO> updateNotificationStatus(@Path("id") String id, @Body Map<String, Boolean> body);

    @DELETE("notifications/{id}")
    Call<Void> deleteNotification(@Path("id") String id);
}