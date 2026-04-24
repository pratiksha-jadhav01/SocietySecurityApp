package com.example.securiodb.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Notice {
    @DocumentId
    private String noticeId;
    private String title;
    private String message;
    private String createdBy;
    private Timestamp timestamp;

    public Notice() {
        // Required for Firestore
    }

    public Notice(String title, String message, String createdBy, Timestamp timestamp) {
        this.title = title;
        this.message = message;
        this.createdBy = createdBy;
        this.timestamp = timestamp;
    }

    public String getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(String noticeId) {
        this.noticeId = noticeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
