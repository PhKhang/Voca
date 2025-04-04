package com.example.voca.ui.dashboard;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.example.voca.bus.PostBUS;
import com.example.voca.dto.PostDTO;


import java.util.ArrayList;
import java.util.List;

public class SharedPostViewModel extends ViewModel {
    private final PostBUS postBUS = new PostBUS();
    private final MutableLiveData<List<PostDTO>> allPosts = new MutableLiveData<>();
    private final MutableLiveData<List<PostDTO>> userPosts = new MutableLiveData<>();
    public void fetchAllPosts() {
        Log.d("DashboardViewModel", "fetchPosts() called");
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> postList) {
                Log.d("DashboardViewModel", "postsLiveData updated");
                allPosts.postValue(postList);
            }

            @Override
            public void onError(String error) {
                Log.d("DashboardViewModel", "posts error");
                // errorLiveData.postValue(error);
            }
        });
    }

    public void fetchUserPosts(String userId) {
        postBUS.fetchPostsByUserId(userId, new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> postList) {
                userPosts.postValue(postList);
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    public LiveData<List<PostDTO>> getAllPostsLiveData() {
        return allPosts;
    }

    public LiveData<List<PostDTO>> getUserPostsLiveData() {
        return userPosts;
    }

    public void updatePost(PostDTO updatedPost) {
        updateList(allPosts, updatedPost);
        updateList(userPosts, updatedPost);
    }

    private void updateList(MutableLiveData<List<PostDTO>> liveData, PostDTO updatedPost) {
        if (liveData.getValue() == null) return;
        List<PostDTO> list = new ArrayList<>(liveData.getValue());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).get_id().equals(updatedPost.get_id())) {
                list.set(i, updatedPost);
                break;
            }
        }
        liveData.setValue(list);
    }
}