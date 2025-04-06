package com.example.voca.dto;

public class NotificationDTO {
    private String _id;
    private UserDTO recipient_id; // Người nhận thông báo
    private UserDTO sender_id;    // Người gửi thông báo
    private PostDTO post_id;      // Bài đăng liên quan
    private String type;          // Loại thông báo (like, comment, follow, v.v.)
    private boolean is_read;      // Trạng thái đã đọc
    private String created_at;    // Thời gian tạo

    // Constructor
    public NotificationDTO(String _id, UserDTO recipient_id, UserDTO sender_id, PostDTO post_id,
                           String type, boolean is_read, String created_at) {
        this._id = _id;
        this.recipient_id = recipient_id;
        this.sender_id = sender_id;
        this.post_id = post_id;
        this.type = type;
        this.is_read = is_read;
        this.created_at = created_at;
    }

    // Getters and Setters
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public UserDTO getRecipient_id() { return recipient_id; }
    public void setRecipient_id(UserDTO recipient_id) { this.recipient_id = recipient_id; }

    public UserDTO getSender_id() { return sender_id; }
    public void setSender_id(UserDTO sender_id) { this.sender_id = sender_id; }

    public PostDTO getPost_id() { return post_id; }
    public void setPost_id(PostDTO post_id) { this.post_id = post_id; }

    public String getTypeNoti() { return type; }
    public void setTypeNoti(String type) { this.type = type; }

    public boolean isIs_read() { return is_read; }
    public void setIs_read(boolean is_read) { this.is_read = is_read; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}