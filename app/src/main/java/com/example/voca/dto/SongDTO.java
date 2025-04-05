package com.example.voca.dto;

import android.util.Log;

public class SongDTO {
    private String _id;
    private String youtube_id;
    private String title;
    private String mp3_file;
    private String thumbnail;
    private UserDTO uploaded_by;
    private String created_at;
    private int recorded_people;
    public SongDTO(String _id, String youtube_id, String title, String mp3_file, String thumbnail, UserDTO uploaded_by, String created_at, int recorded_people) {
        this._id = _id;
        this.youtube_id = youtube_id;
        this.title = title;
        this.thumbnail = thumbnail;
        this.uploaded_by = uploaded_by;
        this.created_at = created_at;
        this.mp3_file = mp3_file;
        this.recorded_people = recorded_people;
    }

    // Getters and Setters
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getYoutube_id() { return youtube_id; }
    public void setYoutube_id(String youtube_id) { this.youtube_id = youtube_id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMp3_file() { return mp3_file; }
    public void setMp3_file(String mp3_file) { this.mp3_file = mp3_file; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public UserDTO getUploaded_by() { return uploaded_by; }
    public void setUploaded_by(UserDTO uploaded_by) { this.uploaded_by = uploaded_by; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public int getRecorded_people() { return recorded_people; }
    public void setRecorded_people(int recorded_people) { this.recorded_people = recorded_people; }
}