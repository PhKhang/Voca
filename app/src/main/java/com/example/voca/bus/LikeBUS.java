package com.example.voca.bus;

import com.example.voca.dao.LikeDAO;
import com.example.voca.dto.LikeDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class LikeBUS {
    private LikeDAO likeDAO;

    public LikeBUS() {
        likeDAO = new LikeDAO();
    }

    // Lấy danh sách lượt thích
    public void fetchLikes(final OnLikesFetchedListener listener) {
        likeDAO.getLikes(new Callback<List<LikeDTO>>() {
            @Override
            public void onResponse(Call<List<LikeDTO>> call, Response<List<LikeDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onLikesFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy danh sách lượt thích: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<LikeDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Tạo mới một lượt thích
    public void createLike(LikeDTO like, final OnLikeCreatedListener listener) {
        likeDAO.createLike(like, new Callback<LikeDTO>() {
            @Override
            public void onResponse(Call<LikeDTO> call, Response<LikeDTO> response) {
                if (response.isSuccessful()) {
                    listener.onLikeCreated(response.body());
                } else {
                    listener.onError("Lỗi khi tạo lượt thích: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LikeDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Cập nhật thông tin lượt thích
    public void updateLike(String id, LikeDTO like, final OnLikeUpdatedListener listener) {
        likeDAO.updateLike(id, like, new Callback<LikeDTO>() {
            @Override
            public void onResponse(Call<LikeDTO> call, Response<LikeDTO> response) {
                if (response.isSuccessful()) {
                    listener.onLikeUpdated(response.body());
                } else {
                    listener.onError("Lỗi khi cập nhật lượt thích: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LikeDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Xóa lượt thích
    public void deleteLike(String id, final OnLikeDeletedListener listener) {
        likeDAO.deleteLike(id, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onLikeDeleted();
                } else {
                    listener.onError("Lỗi khi xóa lượt thích: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Các interface listener
    public interface OnLikesFetchedListener {
        void onLikesFetched(List<LikeDTO> likes);
        void onError(String error);
    }

    public interface OnLikeCreatedListener {
        void onLikeCreated(LikeDTO like);
        void onError(String error);
    }

    public interface OnLikeUpdatedListener {
        void onLikeUpdated(LikeDTO like);
        void onError(String error);
    }

    public interface OnLikeDeletedListener {
        void onLikeDeleted();
        void onError(String error);
    }
}