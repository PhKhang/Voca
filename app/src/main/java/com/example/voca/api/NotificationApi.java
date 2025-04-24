package com.example.voca.api;

import com.example.voca.dto.NotificationDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;

import java.util.List;

public interface NotificationApi {
    @GET("notifications/{userId}")
    Call<List<NotificationDTO>> getNotificationsByUserId(@Path("userId") String userId);

    @PUT("notifications/{id}/read")
    Call<NotificationDTO> markNotificationAsRead(@Path("id") String id);

    @PUT("notifications/{id}/unread")
    Call<NotificationDTO> markNotificationAsUnread(@Path("id") String id);

    @DELETE("notifications/{id}")
    Call<Void> deleteNotification(@Path("id") String id);
}