package com.example.voca.api;

import com.example.voca.dto.LikeDTO;
import com.example.voca.dto.UserDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

public interface LikeApi {
    @GET("likes")
    Call<List<LikeDTO>> getLikes();

    @GET("likes")
    Call<List<LikeDTO>> checkLike(@Query("post_id") String postId, @Query("user_id") String userId);

    @POST("likes")
    Call<LikeDTO> createLike(@Body LikeDTO like);

    @PUT("likes/{id}")
    Call<LikeDTO> updateLike(@Path("id") String id, @Body LikeDTO like);

    @DELETE("likes/{id}")
    Call<Void> deleteLike(@Path("id") String id);
}
