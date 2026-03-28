package com.example.securiodb.models;

public class User {
    private String userId;
    private String name;
    private String email;
    private String role; // admin, guard, owner
    private String flatNumber;

    public User() {} // Required for Firebase

    public User(String userId, String name, String email, String role, String flatNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.flatNumber = flatNumber;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFlatNumber() { return flatNumber; }
}