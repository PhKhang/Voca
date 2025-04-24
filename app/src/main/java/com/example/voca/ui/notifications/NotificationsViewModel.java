package com.example.voca.ui.notifications;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voca.bus.NotificationBUS;
import com.example.voca.dto.NotificationDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationsViewModel extends ViewModel {

    private final MutableLiveData<List<NotificationDTO>> notificationsLiveData = new MutableLiveData<>();
    private final NotificationBUS notificationBUS = new NotificationBUS();
    private List<NotificationDTO> allNotifications = new ArrayList<>();

    public LiveData<List<NotificationDTO>> getNotificationsLiveData() {
        return notificationsLiveData;
    }

    public void fetchNotificationsByUserId(String userId) {
        Log.d("NotificationsViewModel", "Fetching notifications for userId: " + userId);
        notificationBUS.fetchNotificationsByUserId(userId, new NotificationBUS.OnNotificationsFetchedListener() {
            @Override
            public void onNotificationsFetched(List<NotificationDTO> notifications) {
                Log.d("NotificationsViewModel", "Notifications fetched: " + (notifications != null ? notifications.size() : 0));
                allNotifications = notifications != null ? new ArrayList<>(notifications) : new ArrayList<>();
                filterNotifications(0);
            }

            @Override
            public void onError(String error) {
                Log.e("NotificationsViewModel", "Error fetching notifications: " + error);
                notificationsLiveData.setValue(null);
            }
        });
    }

    public void filterNotifications(int tabPosition) {
        List<NotificationDTO> filteredNotifications;
        switch (tabPosition) {
            case 0:
                filteredNotifications = new ArrayList<>(allNotifications);
                break;
            case 1:
                filteredNotifications = allNotifications.stream()
                        .filter(notification -> !notification.isIs_read())
                        .collect(Collectors.toList());
                break;
            case 2:
                filteredNotifications = allNotifications.stream()
                        .filter(NotificationDTO::isIs_read)
                        .collect(Collectors.toList());
                break;
            default:
                filteredNotifications = new ArrayList<>();
                break;
        }
        notificationsLiveData.setValue(filteredNotifications);
    }
}