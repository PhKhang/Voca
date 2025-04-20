package com.example.voca.dao;


import com.example.voca.api.RoomApi;
import com.example.voca.dto.RoomDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class RoomDAO {
//       private static final String BASE_URL = "https://voca-spda.onrender.com/";
     private static final String BASE_URL = "http://10.0.2.2:3000/";
    private RoomApi roomApi;

    public RoomDAO() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        roomApi = retrofit.create(RoomApi.class);
    }

    public void getRooms(Callback<List<RoomDTO>> callback) {
        Call<List<RoomDTO>> call = roomApi.getRooms();
        call.enqueue(callback);
    }

    public void getRoomById(String id, Callback<RoomDTO> callback) {
        Call<RoomDTO> call = roomApi.getRoomById(id);
        call.enqueue(callback);
    }

    public void createRoom(RoomDTO song, Callback<RoomDTO> callback) {
        Call<RoomDTO> call = roomApi.createRoom(song);
        call.enqueue(callback);
    }

    public void searchRooms(String query, Callback<List<RoomDTO>> callback) {
        Call<List<RoomDTO>> call = roomApi.searchRooms(query);
        call.enqueue(callback);
    }

    public void updateRoom(String id, RoomDTO song, Callback<RoomDTO> callback) {
        Call<RoomDTO> call = roomApi.updateRoom(id, song);
        call.enqueue(callback);
    }

    public void deleteRoom(String id, Callback<Void> callback) {
        Call<Void> call = roomApi.deleteRoom(id);
        call.enqueue(callback);
    }
    public void getRoomByCode(String code, Callback<RoomDTO> callback) {
        Call<RoomDTO> call = roomApi.getRoomByCode(code);
        call.enqueue(callback);
    }

    public void getRoomByUserId(String id, Callback<List<RoomDTO>> callback) {
        Call<List<RoomDTO>> call = roomApi.getRoomByUserId(id);
        call.enqueue(callback);
    }
}
