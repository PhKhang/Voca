package com.example.voca.dto;

public class SongDTO {
    private String _id;
    private String youtube_id;
    private String title;
    private String mp3_file;
    private String thumbnail;
    private String uploaded_by;
    private String created_at;

    // Constructor
    public SongDTO(String _id, String youtube_id, String title, String mp3_file, String thumbnail, String uploaded_by, String created_at) {
        this._id = _id;
        this.youtube_id = youtube_id;
        this.title = title;
        this.thumbnail = thumbnail;
        this.uploaded_by = uploaded_by;
        this.created_at = created_at;
        this.mp3_file = mp3_file;
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

    public String getUploaded_by() { return uploaded_by; }
    public void setUploaded_by(String uploaded_by) { this.uploaded_by = uploaded_by; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}