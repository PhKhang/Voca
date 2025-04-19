package com.example.voca.dto;

public class UserDTO {
    private String _id;
    private String firebase_uid;
    private String fcmToken;
    private String username;
    private String email;
    private String avatar;
    private String role;
    private String created_at;
    private String updated_at;
    private int __v;

    // Constructor
    public UserDTO(String _id, String firebase_uid, String username, String email,
                   String avatar, String role, String created_at, String updated_at, int __v) {
        this._id = _id;
        this.firebase_uid = firebase_uid;
        this.username = username;
        this.email = email;
        this.avatar = avatar;
        this.role = role;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.__v = __v;
    }

    // Getters and Setters
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getFirebase_uid() { return firebase_uid; }
    public void setFirebase_uid(String firebase_uid) { this.firebase_uid = firebase_uid; }

    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public String getFcmToken() { return fcmToken; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }

    public int get__v() { return __v; }
    public void set__v(int __v) { this.__v = __v; }
}
