package com.example.voca.api;

import com.example.voca.dto.PostDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;

import java.util.List;

public interface PostApi {
    @GET("posts")
    Call<List<PostDTO>> getPosts();

    @GET("posts/{id}")
    Call<PostDTO> getPostById(@Path("id") String id);

    @POST("posts")
    Call<PostDTO> createPost(@Body PostDTO post);

    @PUT("posts/{id}")
    Call<PostDTO> updatePost(@Path("id") String id, @Body PostDTO post);

    @DELETE("posts/{id}")
    Call<Void> deletePost(@Path("id") String id);
}