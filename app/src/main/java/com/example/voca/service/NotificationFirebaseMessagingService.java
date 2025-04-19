package com.example.voca.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.voca.R;
import com.example.voca.bus.UserBUS;
import com.example.voca.ui.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "my_channel_id";
    private static final String CHANNEL_NAME = "Notifications";
    private static final String ACTION_MARK_AS_READ = "com.example.voca.ACTION_MARK_AS_READ";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("userId", null);
        String recipientId = remoteMessage.getData().get("recipient_id");

        if (currentUserId == null || recipientId == null || !currentUserId.equals(recipientId)) {
            Log.d("NotificationService", "Ignoring notification: not for current user (currentUserId=" + currentUserId + ", recipientId=" + recipientId + ")");
            return;
        }

        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "Thông báo";
        String message = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "Bạn có thông báo mới";
        String notificationId = remoteMessage.getData().get("notification_id");
        Log.d("Noti id", "Notification ID: " + notificationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("navigate_to", "notifications");
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent markAsReadIntent = new Intent(ACTION_MARK_AS_READ);
        markAsReadIntent.setClass(this, MarkAsReadNotificationReceiver.class);
        markAsReadIntent.putExtra("notification_id", notificationId);
        PendingIntent markAsReadPendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId != null ? notificationId.hashCode() : 0, // Sử dụng ID duy nhất
                markAsReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_white)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(contentIntent) // Mở MainActivity khi nhấn và vào Noti Fragment
                .addAction(R.drawable.ic_check_24dp, "Đánh dấu đã đọc", markAsReadPendingIntent) // Thêm action
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Kiểm tra quyền POST_NOTIFICATIONS
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    == PackageManager.PERMISSION_GRANTED) {
                int notifyId = notificationId != null ? notificationId.hashCode() : 1;
                manager.notify(notifyId, builder.build());
                Log.d("NotificationService", "Notification sent with ID: " + notifyId);
            } else {
                Log.w("NotificationService", "POST_NOTIFICATIONS permission not granted");
            }
        } else {
            int notifyId = notificationId != null ? notificationId.hashCode() : 1;
            manager.notify(notifyId, builder.build());
            Log.d("NotificationService", "Notification sent with ID: " + notifyId);
        }
    }

    @Override
    public void onNewToken(String token) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId != null) {
            updateFcmToken(userId, token);
        } else {
            Log.w("FCM Token", "Không tìm thấy userId trong SharedPreferences");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("pendingFcmToken", token);
            editor.apply();
        }
    }

    private void updateFcmToken(String userId, String token) {
        UserBUS userBUS = new UserBUS();
        userBUS.updateFcmToken(userId, token, new UserBUS.OnUpdateFcmTokenListener() {
            @Override
            public void onSuccess() {
                Log.d("FCM Token", "Token updated successfully for userId: " + userId);
            }

            @Override
            public void onError(String error) {
                Log.e("FCM Token", "Failed to update token: " + error);
            }
        });
    }
}