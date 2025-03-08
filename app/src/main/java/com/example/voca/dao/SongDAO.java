package com.example.voca.dao;


import com.example.voca.api.SongApi;
import com.example.voca.dto.SongDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class SongDAO {
    private static final String BASE_URL = "https://voca-spda.onrender.com/";
    private SongApi songApi;

    public SongDAO() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        songApi = retrofit.create(SongApi.class);
    }

    public void getSongs(Callback<List<SongDTO>> callback) {
        Call<List<SongDTO>> call = songApi.getSongs();
        call.enqueue(callback);
    }

    public void getSongById(String id, Callback<SongDTO> callback) {
        Call<SongDTO> call = songApi.getSongById(id);
        call.enqueue(callback);
    }

    public void createSong(SongDTO song, Callback<SongDTO> callback) {
        Call<SongDTO> call = songApi.createSong(song);
        call.enqueue(callback);
    }

    public void updateSong(String id, SongDTO song, Callback<SongDTO> callback) {
        Call<SongDTO> call = songApi.updateSong(id, song);
        call.enqueue(callback);
    }

    public void deleteSong(String id, Callback<Void> callback) {
        Call<Void> call = songApi.deleteSong(id);
        call.enqueue(callback);
    }
}
