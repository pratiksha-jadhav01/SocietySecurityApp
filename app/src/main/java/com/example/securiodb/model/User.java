package com.example.securiodb.model;

public class User {
    public String userId;
    public String name;
    public String email;
    public String role;
    public String flatNumber;

    public User() {
        // Required for Firestore
    }

    public User(String userId, String name, String email, String role, String flatNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.flatNumber = flatNumber;
    }
}
