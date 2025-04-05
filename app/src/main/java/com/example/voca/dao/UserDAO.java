package com.example.voca.dao;

import com.example.voca.api.UserApi;
import com.example.voca.dto.UserDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class UserDAO {
    private static final String BASE_URL = "https://voca-spda.onrender.com/";
    private UserApi userApi;

    public UserDAO() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        userApi = retrofit.create(UserApi.class);
    }

    public void getUsers(Callback<List<UserDTO>> callback) {
        Call<List<UserDTO>> call = userApi.getUsers();
        call.enqueue(callback);
    }

    public void getUserById(String id, Callback<UserDTO> callback) {
        Call<UserDTO> call = userApi.getUserById(id);
        call.enqueue(callback);
    }
    public void getUserByFirebaseUID(String firebaseUID, Callback<List<UserDTO>> callback) {
        Call<List<UserDTO>> call = userApi.getUserByFirebaseUID(firebaseUID);
        call.enqueue(callback);
    }

    public void createUser(UserDTO user, Callback<UserDTO> callback) {
        Call<UserDTO> call = userApi.createUser(user);
        call.enqueue(callback);
    }

    public void updateUser(String id, UserDTO user, Callback<UserDTO> callback) {
        Call<UserDTO> call = userApi.updateUser(id, user);
        call.enqueue(callback);
    }

    public void deleteUser(String id, Callback<Void> callback) {
        Call<Void> call = userApi.deleteUser(id);
        call.enqueue(callback);
    }
}