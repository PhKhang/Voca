package com.example.voca.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.voca.R;
import com.example.voca.bus.NotificationBUS;
import com.example.voca.dto.NotificationDTO;
import com.example.voca.ui.dashboard.DashboardFragment;
import com.example.voca.ui.profile.ProfileViewActivity;

import java.util.List;

public class NotificationAdapter extends BaseAdapter {

    private List<NotificationDTO> notificationList;
    private Context context;
    private NotificationBUS notificationBUS = new NotificationBUS();
    private LayoutInflater inflater;

    public NotificationAdapter(List<NotificationDTO> notificationList, Context context) {
        this.notificationList = notificationList;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return notificationList.size();
    }

    @Override
    public Object getItem(int position) {
        return notificationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.notification_item_layout, parent, false);
            holder = new ViewHolder();
            holder.avatar = convertView.findViewById(R.id.avatar);
            holder.textUsername = convertView.findViewById(R.id.textUsername);
            holder.content = convertView.findViewById(R.id.content);
            holder.textTime = convertView.findViewById(R.id.textTime);
            holder.textNotiContent = convertView.findViewById(R.id.textNotiContent);
            holder.iconAction = convertView.findViewById(R.id.iconAction);
            holder.btnEditPost = convertView.findViewById(R.id.btn_edit_post);
            holder.readIndicator = convertView.findViewById(R.id.read); // Thêm view read
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        NotificationDTO notification = notificationList.get(position);

        holder.textUsername.setText(notification.getSender_id().getUsername());
        holder.textTime.setText(DashboardFragment.TimeFormatter.formatTime(notification.getCreated_at()));

        String content = "";
        int iconResId = R.drawable.ic_heart_filled_24dp;
        switch (notification.getTypeNoti()) {
            case "like":
                content = "đã thích bản thu âm của bạn";
                iconResId = R.drawable.ic_heart_filled_24dp;
                break;
            case "comment":
                content = "đã bình luận về bản thu âm của bạn";
                iconResId = R.drawable.ic_karaoke_24dp;
                break;
            case "follow":
                content = "đã theo dõi bạn";
                iconResId = R.drawable.ic_account_24dp;
                break;
            default:
                content = "đã thực hiện một hành động";
                break;
        }
        holder.textNotiContent.setText(content);
        holder.iconAction.setImageResource(iconResId);

        holder.readIndicator.setVisibility(notification.isIs_read()? View.GONE : View.VISIBLE);

        Glide.with(context)
                .load(notification.getSender_id().getAvatar())
                .placeholder(R.drawable.ic_profile_2_24dp)
                .error(R.drawable.ic_profile_2_24dp)
                .into(holder.avatar);

        holder.avatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileViewActivity.class);
            intent.putExtra("user_id", notification.getSender_id().get_id());
            context.startActivity(intent);
        });

        holder.content.setOnClickListener(v -> {
            String priorityPostId;
            try {
                priorityPostId = notification.getPost_id().get_id();
            } catch (NullPointerException e) {
                Toast.makeText(context, "Không tìm thấy bài đăng", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("priorityPostId", priorityPostId);

            try {
                Navigation.findNavController(v).navigate(
                        R.id.action_notificationsFragment_to_dashboardFragment,
                        args
                );
            } catch (Exception e) {
                Log.e("NotificationAdapter", "Navigation failed: " + e.getMessage(), e);
                Toast.makeText(context, "Không thể điều hướng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý nút chỉnh sửa/xóa thông báo
        holder.btnEditPost.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.getMenuInflater().inflate(R.menu.notification_option_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.option_mark_read) {
                    notification.setIs_read(true);
                    notificationBUS.markNotificationAsRead(notification.get_id(), new NotificationBUS.OnNotificationUpdatedListener() {
                        @Override
                        public void onNotificationUpdated(NotificationDTO updatedNotification) {
                            Toast.makeText(context, "Đã đánh dấu là đã đọc", Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(context, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                    return true;
                } else if (item.getItemId() == R.id.option_delete) {
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Xóa thông báo")
                            .setMessage("Bạn có chắc muốn xóa thông báo này?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                notificationBUS.deleteNotification(notification.get_id(), new NotificationBUS.OnNotificationDeletedListener() {
                                    @Override
                                    public void onNotificationDeleted() {
                                        notificationList.remove(position);
                                        notifyDataSetChanged();
                                        Toast.makeText(context, "Xóa thông báo thành công!", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(context, "Xóa thông báo thất bại: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });

        return convertView;
    }

    public void updateData(List<NotificationDTO> newNotifications) {
        notificationList.clear();
        notificationList.addAll(newNotifications);
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView avatar, iconAction;
        LinearLayout content;
        TextView textUsername, textTime, textNotiContent;
        Button btnEditPost;
        View readIndicator; // Thêm biến cho view read
    }
}