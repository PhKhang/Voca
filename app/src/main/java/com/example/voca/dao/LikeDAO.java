package com.example.voca.dao;

import android.util.Log;

import com.example.voca.api.LikeApi;
import com.example.voca.dto.LikeDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class LikeDAO {
    private static final String BASE_URL = "https://voca-spda.onrender.com/";
    private LikeApi likeApi;

    public LikeDAO() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        likeApi = retrofit.create(LikeApi.class);
    }

    public void getLikes(Callback<List<LikeDTO>> callback) {
        Call<List<LikeDTO>> call = likeApi.getLikes();
        call.enqueue(callback);
    }

    public void createLike(LikeDTO like, Callback<LikeDTO> callback) {
        Call<LikeDTO> call = likeApi.createLike(like);
        call.enqueue(callback);
    }

    public void updateLike(String id, LikeDTO like, Callback<LikeDTO> callback) {
        Call<LikeDTO> call = likeApi.updateLike(id, like);
        call.enqueue(callback);
    }

    public void deleteLike(String id, Callback<Void> callback) {
        Call<Void> call = likeApi.deleteLike(id);
        call.enqueue(callback);
    }
    public void checkLike(String postId, String userId, Callback<List<LikeDTO>> callback) {
        Log.d("CheckingLike", "3");
        Call<List<LikeDTO>> call = likeApi.checkLike(postId, userId);
        call.enqueue(callback);
    }
}