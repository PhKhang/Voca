package com.example.voca.ui.notifications;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voca.bus.NotificationBUS;
import com.example.voca.dto.NotificationDTO;

import java.util.List;

public class NotificationsViewModel extends ViewModel {

    private final MutableLiveData<List<NotificationDTO>> notificationsLiveData = new MutableLiveData<>();
    private final NotificationBUS notificationBUS = new NotificationBUS();

    public LiveData<List<NotificationDTO>> getNotificationsLiveData() {
        return notificationsLiveData;
    }

    public void fetchNotificationsByUserId(String userId) {
        Log.d("NotificationsViewModel", "Fetching notifications for userId: " + userId);
        notificationBUS.fetchNotificationsByUserId(userId, new NotificationBUS.OnNotificationsFetchedListener() {
            @Override
            public void onNotificationsFetched(List<NotificationDTO> notifications) {
                Log.d("NotificationsViewModel", "Notifications fetched: " + (notifications != null ? notifications.size() : 0));
                notificationsLiveData.setValue(notifications);
            }

            @Override
            public void onError(String error) {
                Log.e("NotificationsViewModel", "Error fetching notifications: " + error);
                notificationsLiveData.setValue(null);
            }
        });
    }
}