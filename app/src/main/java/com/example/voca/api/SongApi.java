package com.example.voca.api;

import com.example.voca.dto.SongDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;

import java.util.List;

public interface SongApi {
    @GET("songs")
    Call<List<SongDTO>> getSongs();

    @GET("songs/{id}")
    Call<SongDTO> getSongById(@Path("id") String id);

    @POST("songs")
    Call<SongDTO> createSong(@Body SongDTO song);

    @PUT("songs/{id}")
    Call<SongDTO> updateSong(@Path("id") String id, @Body SongDTO song);

    @DELETE("songs/{id}")
    Call<Void> deleteSong(@Path("id") String id);
}