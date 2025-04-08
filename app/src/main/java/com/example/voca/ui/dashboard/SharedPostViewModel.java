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
    public void fetchAllPosts(String priorityPostId) {
        Log.d("DashboardViewModel", "fetchPosts() called with priorityPostId: " + priorityPostId);
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> postList) {
                Log.d("DashboardViewModel", "postsLiveData updated");
                List<PostDTO> sortedList = sortWithPriority(postList, priorityPostId);
                allPosts.postValue(sortedList);
            }

            @Override
            public void onError(String error) {
                Log.d("DashboardViewModel", "posts error: " + error);
            }
        });
    }

    public void fetchAllPosts() {
        fetchAllPosts(null);
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

    private List<PostDTO> sortWithPriority(List<PostDTO> postList, String priorityPostId) {
        if (postList == null || postList.isEmpty() || priorityPostId == null || priorityPostId.isEmpty()) {
            return postList;
        }

        int priorityIndex = -1;
        for (int i = 0; i < postList.size(); i++) {
            if (priorityPostId.equals(postList.get(i).get_id())) {
                priorityIndex = i;
                break;
            }
        }

        if (priorityIndex > 0) {  // Chỉ swap nếu không phải ở vị trí đầu
            PostDTO temp = postList.get(0);
            postList.set(0, postList.get(priorityIndex));
            postList.set(priorityIndex, temp);
        }

        return postList;
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