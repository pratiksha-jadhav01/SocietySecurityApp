package com.example.securiodb.model;

import com.google.firebase.Timestamp;

public class Visitor {
    public String visitorId;
    public String name;
    public String phone;
    public String flatNumber;
    public String purpose;
    public String photoUrl;
    public String status; // Pending, Approved, Rejected
    public String createdBy;
    public String approvedBy;
    public Timestamp entryTime;
    public Timestamp exitTime;
    public Timestamp timestamp;

    public Visitor() {
        // Required for Firestore
    }

    public Visitor(String visitorId, String name, String phone, String flatNumber, String purpose, 
                   String photoUrl, String status, String createdBy, Timestamp timestamp) {
        this.visitorId = visitorId;
        this.name = name;
        this.phone = phone;
        this.flatNumber = flatNumber;
        this.purpose = purpose;
        this.photoUrl = photoUrl;
        this.status = status;
        this.createdBy = createdBy;
        this.timestamp = timestamp;
    }
}
