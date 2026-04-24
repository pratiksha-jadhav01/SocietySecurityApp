package com.example.securiodb.models;

public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String role; // admin, guard, owner
    private String flatNumber;
    private String profileImageUrl;

    public User() {} // Required for Firebase

    public User(String userId, String name, String email, String role, String flatNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.flatNumber = flatNumber;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    // Alias for compatibility
    public String getUid() { return userId; }
    public void setUid(String uid) { this.userId = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}