package com.example.voca.dto;

public class PostDTO {
    private String _id;
    private String user_id; // ID của User
    private String song_id; // ID của Song
    private String audio_url;
    private String caption;
    private int likes;
    private String created_at;

    // Constructor
    public PostDTO(String _id, String user_id, String song_id, String audio_url, String caption, int likes, String created_at) {
        this._id = _id;
        this.user_id = user_id;
        this.song_id = song_id;
        this.audio_url = audio_url;
        this.caption = caption;
        this.likes = likes;
        this.created_at = created_at;
    }

    // Getters and Setters
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public String getSong_id() { return song_id; }
    public void setSong_id(String song_id) { this.song_id = song_id; }

    public String getAudio_url() { return audio_url; }
    public void setAudio_url(String audio_url) { this.audio_url = audio_url; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}