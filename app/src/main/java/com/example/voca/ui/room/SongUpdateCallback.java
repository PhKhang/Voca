package com.example.voca.ui.room;

import com.example.voca.dto.SongDTO;

import java.util.List;

public interface SongUpdateCallback {
    List<SongDTO> getQueue();
    void addSong(SongDTO song);
    void removeSong(String songId);
    void singSong(String songId);
}
