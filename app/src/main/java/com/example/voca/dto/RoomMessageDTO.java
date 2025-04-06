package com.example.voca.dto;

import java.util.List;

public class RoomMessageDTO {
    private String message_type;
    private UserDTO user_id;
    private String message;
    private String timestamp;

    // Constructor
    public RoomMessageDTO(String message_type, UserDTO user_id, String message, String timestamp) {
        this.message_type = message_type;
        this.user_id = user_id;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public UserDTO getUser_id() {
        return user_id;
    }

    public void setUser_id(UserDTO user_id) {
        this.user_id = user_id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
