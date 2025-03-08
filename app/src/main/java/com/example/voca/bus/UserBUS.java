package com.example.voca.bus;

import android.util.Log;

import com.example.voca.dao.UserDAO;
import com.example.voca.dto.UserDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;


public class UserBUS {
    private UserDAO userDAO;

    public UserBUS() {
        userDAO = new UserDAO();
    }

    // Lấy danh sách người dùng
    public void fetchUsers(final OnUsersFetchedListener listener) {
        userDAO.getUsers(new Callback<List<UserDTO>>() {
            @Override
            public void onResponse(Call<List<UserDTO>> call, Response<List<UserDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onUsersFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy danh sách người dùng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<UserDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
                Log.e("Error", "Lỗi khi lấy danh sách người dùng: " + t.getMessage());
            }
        });
    }

    // Lấy thông tin một người dùng theo ID
    public void fetchUserById(String id, final OnUserFetchedListener listener) {
        userDAO.getUserById(id, new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                if (response.isSuccessful()) {
                    listener.onUserFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy người dùng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Tạo mới một người dùng
    public void createUser(UserDTO user, final OnUserCreatedListener listener) {
        userDAO.createUser(user, new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                if (response.isSuccessful()) {
                    listener.onUserCreated(response.body());
                } else {
                    listener.onError("Lỗi khi tạo người dùng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Cập nhật thông tin người dùng
    public void updateUser(String id, UserDTO user, final OnUserUpdatedListener listener) {
        userDAO.updateUser(id, user, new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                if (response.isSuccessful()) {
                    listener.onUserUpdated(response.body());
                } else {
                    listener.onError("Lỗi khi cập nhật người dùng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Xóa người dùng
    public void deleteUser(String id, final OnUserDeletedListener listener) {
        userDAO.deleteUser(id, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onUserDeleted();
                } else {
                    listener.onError("Lỗi khi xóa người dùng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Các interface listener để xử lý kết quả
    public interface OnUsersFetchedListener {
        void onUsersFetched(List<UserDTO> users);
        void onError(String error);
    }

    public interface OnUserFetchedListener {
        void onUserFetched(UserDTO user);
        void onError(String error);
    }

    public interface OnUserCreatedListener {
        void onUserCreated(UserDTO user);
        void onError(String error);
    }

    public interface OnUserUpdatedListener {
        void onUserUpdated(UserDTO user);
        void onError(String error);
    }

    public interface OnUserDeletedListener {
        void onUserDeleted();
        void onError(String error);
    }
}