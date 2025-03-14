package com.example.voca.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voca.bus.UserBUS;
import com.example.voca.dto.UserDTO;

import java.util.HashMap;
import java.util.Map;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<Map<String, String>> usernameCache = new MutableLiveData<>(new HashMap<>());
    private final UserBUS userBUS;

    public UserViewModel() {
        userBUS = new UserBUS();
    }

    public LiveData<String> getUsername(String userId) {
        MutableLiveData<String> usernameLiveData = new MutableLiveData<>();
            Log.d("DEBUG", "Fetching user for ID: " + userId);
        // Kiểm tra cache trước
        if (usernameCache.getValue().containsKey(userId)) {
            usernameLiveData.setValue(usernameCache.getValue().get(userId));
        } else {
            userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
                @Override
                public void onUserFetched(UserDTO user) {
                    Map<String, String> cache = usernameCache.getValue();
                    cache.put(userId, user.getUsername());
                    usernameCache.postValue(cache);
                    usernameLiveData.postValue(user.getUsername());
                }

                @Override
                public void onError(String error) {
                    Log.d("UserViewModel error", error);
                    usernameLiveData.postValue("Unknown");
                }
            });
        }

        return usernameLiveData;
    }
}