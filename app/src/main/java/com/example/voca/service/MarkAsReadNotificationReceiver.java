package com.example.voca.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.voca.bus.NotificationBUS;
import com.example.voca.dto.NotificationDTO;

public class MarkAsReadNotificationReceiver extends BroadcastReceiver {
    private static final String ACTION_MARK_AS_READ = "com.example.voca.ACTION_MARK_AS_READ";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_MARK_AS_READ.equals(intent.getAction())) {
            Log.w("MarkAsReadReceiver", "Ignoring broadcast with incorrect action: " + intent.getAction());
            return;
        }

        String notificationId = intent.getStringExtra("notification_id");
        if (notificationId == null) {
            Log.e("MarkAsReadReceiver", "No notification_id provided");
            return;
        }

        NotificationBUS notificationBUS = new NotificationBUS();
        notificationBUS.markNotificationAsRead(notificationId, new NotificationBUS.OnNotificationUpdatedListener() {
            @Override
            public void onNotificationUpdated(NotificationDTO updatedNotification) {
                Log.d("MarkAsReadReceiver", "Notification marked as read: " + notificationId);
                Toast.makeText(context, "Đã đánh dấu là đã đọc", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Log.e("MarkAsReadReceiver", "Failed to mark notification as read: " + error);
                Toast.makeText(context, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}