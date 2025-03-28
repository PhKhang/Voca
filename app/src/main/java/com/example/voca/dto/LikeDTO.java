package com.example.voca.dto;

public class LikeDTO {
    private String _id;
    private PostDTO post_id; // ID của Post
    private UserDTO user_id; // ID của User
    private String created_at;

    // Constructor
    public LikeDTO(String _id, PostDTO post_id, UserDTO user_id, String created_at) {
        this._id = _id;
        this.post_id = post_id;
        this.user_id = user_id;
        this.created_at = created_at;
    }

    // Getters and Setters
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public PostDTO getPost_id() { return post_id; }
    public void setPost_id(PostDTO post_id) { this.post_id = post_id; }

    public UserDTO getUser_id() { return user_id; }
    public void setUser_id(UserDTO user_id) { this.user_id = user_id; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
