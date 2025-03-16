package com.example.voca.ui.dashboard;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voca.bus.LikeBUS;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dao.PostDAO;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final PostBUS postBUS;
    private final UserBUS userBUS;

    private final MutableLiveData<List<PostDTO>> postsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData =  new MutableLiveData<>();

    public DashboardViewModel() {
        postBUS = new PostBUS();
        userBUS = new UserBUS();
        fetchPosts();
    }

    public void fetchPosts() {
        Log.d("DashboardViewModel", "fetchPosts() called");
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> postList) {
                Log.d("DashboardViewModel", "postsLiveData updated");
                postsLiveData.postValue(postList);
            }

            @Override
            public void onError(String error) {
                Log.d("DashboardViewModel", "posts error");
                errorLiveData.postValue(error);
            }
        });
    }
    public LiveData<List<PostDTO>> getPostsLiveData() {
        return postsLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }
}