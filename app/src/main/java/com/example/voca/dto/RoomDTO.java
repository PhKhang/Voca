package com.example.voca.dto;

import java.util.List;

public class RoomDTO {
    private String _id;
    private String name;
    private String code;
    private Boolean is_private;
    private String description;
    private String created_at;
    private String updated_at;
    private UserDTO created_by;
    private List<UserDTO> members;
    private List<SongDTO> queue;
    private SongDTO current_song;
    private String current_song_start_time;
    private List<RoomMessageDTO> chats;

    // Constructor
    public RoomDTO(String _id, String name, String code, Boolean is_private, String description,
                   String created_at, String updated_at, UserDTO created_by, List<UserDTO> members,
                   List<SongDTO> queue, SongDTO current_song, String current_song_start_time,
                   List<RoomMessageDTO> chats) {
        this._id = _id;
        this.name = name;
        this.code = code;
        this.is_private = is_private;
        this.description = description;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.created_by = created_by;
        this.members = members;
        this.queue = queue;
        this.current_song = current_song;
        this.current_song_start_time = current_song_start_time;
        this.chats = chats;
    }

    // Getters and Setters

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getIs_private() {
        return is_private;
    }

    public void setIs_private(Boolean is_private) {
        this.is_private = is_private;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public UserDTO getCreated_by() {
        return created_by;
    }

    public void setCreated_by(UserDTO created_by) {
        this.created_by = created_by;
    }

    public List<UserDTO> getMembers() {
        return members;
    }

    public void setMembers(List<UserDTO> members) {
        this.members = members;
    }

    public List<SongDTO> getQueue() {
        return queue;
    }

    public void setQueue(List<SongDTO> queue) {
        this.queue = queue;
    }

    public SongDTO getCurrent_song() {
        return current_song;
    }

    public void setCurrent_song(SongDTO current_song) {
        this.current_song = current_song;
    }

    public String getCurrent_song_start_time() {
        return current_song_start_time;
    }

    public void setCurrent_song_start_time(String current_song_start_time) {
        this.current_song_start_time = current_song_start_time;
    }

    public List<RoomMessageDTO> getChats() {
        return chats;
    }

    public void setChats(List<RoomMessageDTO> chats) {
        this.chats = chats;
    }
}
