package com.example.voca.api;

import com.example.voca.dto.LikeDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;

import java.util.List;

public interface LikeApi {
    @GET("likes")
    Call<List<LikeDTO>> getLikes();

    @POST("likes")
    Call<LikeDTO> createLike(@Body LikeDTO like);

    @PUT("likes/{id}")
    Call<LikeDTO> updateLike(@Path("id") String id, @Body LikeDTO like);

    @DELETE("likes/{id}")
    Call<Void> deleteLike(@Path("id") String id);
}
