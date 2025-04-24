package com.example.voca.bus;

import android.util.Log;

import com.example.voca.dao.RoomDAO;
import com.example.voca.dto.RoomDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class RoomBUS {
    private RoomDAO roomDAO;

    public RoomBUS() {
        roomDAO = new RoomDAO();
    }

    // Lấy danh sách phòng
    public void fetchRooms(final OnRoomsFetchedListener listener) {
        roomDAO.getRooms(new Callback<List<RoomDTO>>() {
            @Override
            public void onResponse(Call<List<RoomDTO>> call, Response<List<RoomDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onRoomsFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy danh sách phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<RoomDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
                Log.e("Error", "Lỗi khi lấy danh sách phòng: " + t.getMessage());
            }
        });
    }

    // Lấy thông tin một phòng theo ID
    public void fetchRoomById(String id, final OnRoomFetchedListener listener) {
        roomDAO.getRoomById(id, new Callback<RoomDTO>() {
            @Override
            public void onResponse(Call<RoomDTO> call, Response<RoomDTO> response) {
                if (response.isSuccessful()) {
                    listener.onRoomFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RoomDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Lấy thông tin một phòng theo Code
    public void fetchRoomByCode(String code, final OnRoomFetchedListener listener) {
        roomDAO.getRoomByCode(code, new Callback<RoomDTO>() {
            @Override
            public void onResponse(Call<RoomDTO> call, Response<RoomDTO> response) {
                if (response.isSuccessful()) {
                    listener.onRoomFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy phòng bằng code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RoomDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Lấy danh sách phòng theo User ID
    public void fetchRoomsByUserId(String userId, final OnRoomsFetchedListener listener) {
        roomDAO.getRoomByUserId(userId, new Callback<List<RoomDTO>>() {
            @Override
            public void onResponse(Call<List<RoomDTO>> call, Response<List<RoomDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onRoomsFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy danh sách phòng theo user ID: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<RoomDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Tìm kiếm phòng
    public void searchRooms(String query, final OnRoomsFetchedListener listener) {
        roomDAO.searchRooms(query, new Callback<List<RoomDTO>>() {
            @Override
            public void onResponse(Call<List<RoomDTO>> call, Response<List<RoomDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onRoomsFetched(response.body());
                } else {
                    listener.onError("Lỗi khi tìm kiếm phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<RoomDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Tạo mới một phòng
    public void createRoom(RoomDTO room, final OnRoomCreatedListener listener) {
        roomDAO.createRoom(room, new Callback<RoomDTO>() {
            @Override
            public void onResponse(Call<RoomDTO> call, Response<RoomDTO> response) {
                if (response.isSuccessful()) {
                    listener.onRoomCreated(response.body());
                } else {
                    listener.onError("Lỗi khi tạo phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RoomDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Cập nhật thông tin phòng
    public void updateRoom(String id, RoomDTO room, final OnRoomUpdatedListener listener) {
        roomDAO.updateRoom(id, room, new Callback<RoomDTO>() {
            @Override
            public void onResponse(Call<RoomDTO> call, Response<RoomDTO> response) {
                if (response.isSuccessful()) {
                    listener.onRoomUpdated(response.body());
                } else {
                    listener.onError("Lỗi khi cập nhật phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RoomDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Xóa phòng
    public void deleteRoom(String id, final OnRoomDeletedListener listener) {
        roomDAO.deleteRoom(id, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onRoomDeleted();
                } else {
                    listener.onError("Lỗi khi xóa phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Các interface listener để xử lý kết quả
    public interface OnRoomsFetchedListener {
        void onRoomsFetched(List<RoomDTO> rooms);
        void onError(String error);
    }

    public interface OnRoomFetchedListener {
        void onRoomFetched(RoomDTO room);
        void onError(String error);
    }

    public interface OnRoomCreatedListener {
        void onRoomCreated(RoomDTO room);
        void onError(String error);
    }

    public interface OnRoomUpdatedListener {
        void onRoomUpdated(RoomDTO room);
        void onError(String error);
    }

    public interface OnRoomDeletedListener {
        void onRoomDeleted();
        void onError(String error);
    }
}