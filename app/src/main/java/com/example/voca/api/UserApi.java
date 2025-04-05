package com.example.voca.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
    import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.example.voca.dto.UserDTO;

import java.util.List;

public interface UserApi {
    @GET("users")
    Call<List<UserDTO>> getUsers();

    @GET("users/{id}")
    Call<UserDTO> getUserById(@Path("id") String id);

    @GET("users")
    Call<List<UserDTO>> getUserByFirebaseUID(@Query("firebase_uid") String firebase_uid);

    @POST("users")
    Call<UserDTO> createUser(@Body UserDTO user);

    @PUT("users/{id}")
    Call<UserDTO> updateUser(@Path("id") String id, @Body UserDTO user);

    @DELETE("users/{id}")
    Call<Void> deleteUser(@Path("id") String id);
}