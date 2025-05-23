package com.example.voca.api;

import com.example.voca.dto.RoomDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RoomApi {
    @GET("rooms")
    Call<List<RoomDTO>> getRooms();

    @GET("rooms/{id}")
    Call<RoomDTO> getRoomById(@Path("id") String id);
    @GET("rooms")
    Call<List<RoomDTO>> getRoomByUserId(@Query("user_id") String id);

    @GET("rooms")
    Call<RoomDTO> getRoomByCode(@Query("code") String code);

    @GET("rooms")
    Call<List<RoomDTO>> searchRooms( @Query("name") String name);

    @POST("rooms")
    Call<RoomDTO> createRoom(@Body RoomDTO song);

    @PUT("rooms/{id}")
    Call<RoomDTO> updateRoom(@Path("id") String id, @Body RoomDTO song);

    @DELETE("rooms/{id}")
    Call<Void> deleteRoom(@Path("id") String id);
}
