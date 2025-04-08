package com.example.voca.bus;


import com.example.voca.dao.SongDAO;
import com.example.voca.dto.SongDTO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class SongBUS {
    private SongDAO songDAO;

    public SongBUS() {
        songDAO = new SongDAO();
    }

    // Lấy danh sách bài hát
    public void fetchSongs(final OnSongsFetchedListener listener) {
        songDAO.getSongs(new Callback<List<SongDTO>>() {
            @Override
            public void onResponse(Call<List<SongDTO>> call, Response<List<SongDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onSongsFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy danh sách bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SongDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Lấy thông tin một bài hát theo ID
    public void fetchSongById(String id, final OnSongFetchedListener listener) {
        songDAO.getSongById(id, new Callback<SongDTO>() {
            @Override
            public void onResponse(Call<SongDTO> call, Response<SongDTO> response) {
                if (response.isSuccessful()) {
                    listener.onSongFetched(response.body());
                } else {
                    listener.onError("Lỗi khi lấy bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SongDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Tạo mới một bài hát
    public void createSong(SongDTO song, final OnSongCreatedListener listener) {
        songDAO.createSong(song, new Callback<SongDTO>() {
            @Override
            public void onResponse(Call<SongDTO> call, Response<SongDTO> response) {
                if (response.isSuccessful()) {
                    listener.onSongCreated(response.body());
                } else {
                    listener.onError("Lỗi khi tạo bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SongDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Cập nhật thông tin bài hát
    public void updateSong(String id, SongDTO song, final OnSongUpdatedListener listener) {
        songDAO.updateSong(id, song, new Callback<SongDTO>() {
            @Override
            public void onResponse(Call<SongDTO> call, Response<SongDTO> response) {
                if (response.isSuccessful()) {
                    listener.onSongUpdated(response.body());
                } else {
                    listener.onError("Lỗi khi cập nhật bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SongDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Xóa bài hát
    public void deleteSong(String id, final OnSongDeletedListener listener) {
        songDAO.deleteSong(id, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onSongDeleted();
                } else {
                    listener.onError("Lỗi khi xóa bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void searchSongsByTitle(String query, final OnSongsFetchedListener listener) {
        songDAO.searchSongs(query, new Callback<List<SongDTO>>() {
            @Override
            public void onResponse(Call<List<SongDTO>> call, Response<List<SongDTO>> response) {
                if (response.isSuccessful()) {
                    listener.onSongsFetched(response.body());
                } else {
                    listener.onError("Lỗi khi tìm kiếm bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SongDTO>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void recordSong(String id, final OnSongRecordedListener listener) {
        songDAO.recordSong(id, new Callback<SongDTO>() {
            @Override
            public void onResponse(Call<SongDTO> call, Response<SongDTO> response) {
                if (response.isSuccessful()) {
                    listener.onSongRecorded(response.body());
                } else {
                    listener.onError("Lỗi khi ghi âm bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SongDTO> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // Các interface listener
    public interface OnSongsFetchedListener {
        void onSongsFetched(List<SongDTO> songs);
        void onError(String error);
    }

    public interface OnSongFetchedListener {
        void onSongFetched(SongDTO song);
        void onError(String error);
    }

    public interface OnSongCreatedListener {
        void onSongCreated(SongDTO song);
        void onError(String error);
    }

    public interface OnSongUpdatedListener {
        void onSongUpdated(SongDTO song);
        void onError(String error);
    }

    public interface OnSongDeletedListener {
        void onSongDeleted();
        void onError(String error);
    }

    public interface OnSongRecordedListener {
        void onSongRecorded(SongDTO song);
        void onError(String error);
    }
}
