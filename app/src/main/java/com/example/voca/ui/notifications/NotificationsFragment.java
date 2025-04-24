package com.example.voca.ui.notifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.voca.R;
import com.example.voca.databinding.FragmentNotificationsBinding;
import com.example.voca.dto.NotificationDTO;
import com.example.voca.ui.adapter.NotificationAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel notificationsViewModel;
    private NotificationAdapter notificationAdapter;
    private List<NotificationDTO> notificationList = new ArrayList<>();
    private ListView listView;

    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = binding.listViewNotifications;
        notificationAdapter = new NotificationAdapter(notificationList, getContext());
        tabLayout = root.findViewById(R.id.tabLayout);
        listView.setAdapter(notificationAdapter);
        /*listView.setOnItemClickListener((parent, view, position, id) -> {
            NotificationDTO selectedNotification = notificationList.get(position);
            String priorityPostId = selectedNotification.getPost_id().get_id();

            Bundle args = new Bundle();
            args.putString("priorityPostId", priorityPostId);

            NavController navController = Navigation.findNavController(view);
            navController.navigate(
                    R.id.action_notificationsFragment_to_dashboardFragment,
                    args
            );
        });*/

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                resetAndFetchData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                resetAndFetchData(tab.getPosition());
            }
        });

        notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId == null) {
            Toast.makeText(getContext(), "Không tìm thấy userId", Toast.LENGTH_SHORT).show();
            return root;
        }

        notificationsViewModel.fetchNotificationsByUserId(userId);

        notificationsViewModel.getNotificationsLiveData().observe(getViewLifecycleOwner(), notifications -> {
            Log.d("NotificationsFragment", "Received notifications: " + (notifications != null ? notifications.size() : 0));
            if (notifications != null && !notifications.isEmpty()) {
                notificationList.clear();
                notificationList.addAll(notifications);
                notificationAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "Không có thông báo nào", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void resetAndFetchData(int tabPosition) {
        notificationsViewModel.filterNotifications(tabPosition);
    }

}