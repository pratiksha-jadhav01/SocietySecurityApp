package com.example.securiodb.models;

/**
 * Model class for User Payment Status.
 */
public class UserStatusModel {
    private String userId;
    private String status;
    private long updatedAt;

    // Empty constructor for Firebase
    public UserStatusModel() {}

    public UserStatusModel(String userId, String status, long updatedAt) {
        this.userId    = userId;
        this.status    = status;
        this.updatedAt = updatedAt;
    }

    public String getUserId()    { return userId; }
    public String getStatus()    { return status; }
    public long   getUpdatedAt() { return updatedAt; }

    public void setUserId(String userId)       { this.userId = userId; }
    public void setStatus(String status)       { this.status = status; }
    public void setUpdatedAt(long updatedAt)   { this.updatedAt = updatedAt; }
}
