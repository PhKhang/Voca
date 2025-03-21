package com.example.voca.dao;

import android.util.Log;

import com.example.voca.api.PostApi;
import com.example.voca.dto.PostDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class PostDAO {
    private static final String BASE_URL = "https://voca-spda.onrender.com/";
    private PostApi postApi;

    public PostDAO() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        postApi = retrofit.create(PostApi.class);
    }

    public void getPosts(Callback<List<PostDTO>> callback) {
        Call<List<PostDTO>> call = postApi.getPosts();
        call.enqueue(callback);
    }

    public void getPostsByUserId(String user_id, Callback<List<PostDTO>> callback){
        Call<List<PostDTO>> call = postApi.getPostsByUserId(user_id);
        call.enqueue(callback);
    }

    public void getPostById(String id, Callback<PostDTO> callback) {
        Call<PostDTO> call = postApi.getPostById(id);
        call.enqueue(callback);
    }

    public void createPost(PostDTO post, Callback<PostDTO> callback) {
        Call<PostDTO> call = postApi.createPost(post);
        call.enqueue(callback);
    }

    public void updatePost(String id, PostDTO post, Callback<PostDTO> callback) {
        Call<PostDTO> call = postApi.updatePost(id, post);
        call.enqueue(callback);
    }

    public void deletePost(String id, Callback<Void> callback) {
        Call<Void> call = postApi.deletePost(id);
        call.enqueue(callback);
    }
}
