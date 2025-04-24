package com.example.voca.ui.management;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UsersManagementViewModel extends ViewModel {
    private final MutableLiveData<List<UserDTO>> usersLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<PostDTO>> postsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final UserBUS userBUS = new UserBUS();
    private final PostBUS postBUS = new PostBUS();

    public LiveData<List<UserDTO>> getUsersLiveData() {
        return usersLiveData;
    }

    public LiveData<List<PostDTO>> getPostsLiveData() {
        return postsLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void fetchData() {
        AtomicInteger tasksCompleted = new AtomicInteger(0);
        int totalTasks = 2;

        userBUS.fetchUsers(new UserBUS.OnUsersFetchedListener() {
            @Override
            public void onUsersFetched(List<UserDTO> fetchedUsers) {
                usersLiveData.setValue(fetchedUsers != null ? new ArrayList<>(fetchedUsers) : new ArrayList<>());
                if (tasksCompleted.incrementAndGet() == totalTasks) {
                }
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue("Error fetching users: " + error);
            }
        });

        // Fetch posts
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> fetchedPosts) {
                postsLiveData.setValue(fetchedPosts != null ? new ArrayList<>(fetchedPosts) : new ArrayList<>());
                if (tasksCompleted.incrementAndGet() == totalTasks) {
                }
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue("Error fetching posts: " + error);
            }
        });
    }

    public void searchUsers(String query) {
        List<UserDTO> currentUsers = usersLiveData.getValue();
        if (currentUsers == null) return;

        if (query.isEmpty()) {
            usersLiveData.setValue(new ArrayList<>(currentUsers));
        } else {
            List<UserDTO> filteredUsers = new ArrayList<>();
            for (UserDTO user : currentUsers) {
                if (user.getUsername() != null && user.getUsername().toLowerCase().contains(query.toLowerCase())) {
                    filteredUsers.add(user);
                }
            }
            usersLiveData.setValue(filteredUsers);
        }
    }
}