package com.example.voca.bus;

import com.example.voca.dao.NotificationDAO;
import com.example.voca.dto.NotificationDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class NotificationBUS {
    private NotificationDAO notificationDAO;

    public NotificationBUS() {
        notificationDAO = new NotificationDAO();
    }

    // Lấy danh sách thông báo theo userId
    public void fetchNotificationsByUserId(String userId, final OnNotificationsFetchedListener listener) {
        notificationDAO.getNotificationsByUserId(userId, new Callback<List<NotificationDTO>>() {
            @Override
            public void onResponse(Call<List<NotificationDTO>> call, Response<List<NotificationDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onNotificationsFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy danh sách thông báo: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<NotificationDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Đánh dấu thông báo là đã đọc
    public void markNotificationAsRead(String id, final OnNotificationUpdatedListener listener) {
        notificationDAO.markNotificationAsRead(id, new Callback<NotificationDTO>() {
            @Override
            public void onResponse(Call<NotificationDTO> call, Response<NotificationDTO> response) {
                if (response.isSuccessful()) {
                    listener.onNotificationUpdated(response.body());
                } else {
                    listener.onError("Lỗi khi cập nhật thông báo: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<NotificationDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Xóa thông báo
    public void deleteNotification(String id, final OnNotificationDeletedListener listener) {
        notificationDAO.deleteNotification(id, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onNotificationDeleted();
                } else {
                    listener.onError("Lỗi khi xóa thông báo: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Các interface listener
    public interface OnNotificationsFetchedListener {
        void onNotificationsFetched(List<NotificationDTO> notifications);
        void onError(String error);
    }

    public interface OnNotificationUpdatedListener {
        void onNotificationUpdated(NotificationDTO notification);
        void onError(String error);
    }

    public interface OnNotificationDeletedListener {
        void onNotificationDeleted();
        void onError(String error);
    }
}