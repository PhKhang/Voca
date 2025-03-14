package com.example.voca.bus;

import android.util.Log;

import com.example.voca.dao.PostDAO;
import com.example.voca.dto.PostDTO;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class PostBUS {
    private PostDAO postDAO;

    public PostBUS() {
        postDAO = new PostDAO();
    }

    // Lấy danh sách bài đăng
    public void fetchPosts(final OnPostsFetchedListener listener) {
        postDAO.getPosts(new Callback<List<PostDTO>>() {
            @Override
            public void onResponse(Call<List<PostDTO>> call, Response<List<PostDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onPostsFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy danh sách bài đăng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<PostDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Lấy thông tin một bài đăng theo ID
    public void fetchPostById(String id, final OnPostFetchedListener listener) {
        postDAO.getPostById(id, new Callback<PostDTO>() {
            @Override
            public void onResponse(Call<PostDTO> call, Response<PostDTO> response) {
                if (response.isSuccessful()) {
                    listener.onPostFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy bài đăng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PostDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Tạo mới một bài đăng
    public void createPost(PostDTO post, final OnPostCreatedListener listener) {
        postDAO.createPost(post, new Callback<PostDTO>() {
            @Override
            public void onResponse(Call<PostDTO> call, Response<PostDTO> response) {
                if (response.isSuccessful()) {
                    listener.onPostCreated(response.body());
                } else {
                    listener.onError("Lỗi khi tạo bài đăng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PostDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Cập nhật thông tin bài đăng
    public void updatePost(String id, PostDTO post, final OnPostUpdatedListener listener) {
        postDAO.updatePost(id, post, new Callback<PostDTO>() {
            @Override
            public void onResponse(Call<PostDTO> call, Response<PostDTO> response) {
                if (response.isSuccessful()) {
                    listener.onPostUpdated(response.body());
                } else {
                    listener.onError("Lỗi khi cập nhật bài đăng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PostDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Xóa bài đăng
    public void deletePost(String id, final OnPostDeletedListener listener) {
        postDAO.deletePost(id, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onPostDeleted();
                } else {
                    listener.onError("Lỗi khi xóa bài đăng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Các interface listener
    public interface OnPostsFetchedListener {
        void onPostsFetched(List<PostDTO> posts);
        void onError(String error);
    }

    public interface OnPostFetchedListener {
        void onPostFetched(PostDTO post);
        void onError(String error);
    }

    public interface OnPostCreatedListener {
        void onPostCreated(PostDTO post);
        void onError(String error);
    }

    public interface OnPostUpdatedListener {
        void onPostUpdated(PostDTO post);
        void onError(String error);
    }

    public interface OnPostDeletedListener {
        void onPostDeleted();
        void onError(String error);
    }
}