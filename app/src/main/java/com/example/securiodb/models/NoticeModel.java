package com.example.securiodb.models;

/**
 * Model class representing a Society Notice.
 * Used for mapping Firestore documents to Java objects.
 */
public class NoticeModel {

    private String noticeId;
    private String title;
    private String message;
    private String createdBy;
    private long timestamp; // Time in milliseconds

    /**
     * Default constructor required for Firebase Firestore deserialization.
     */
    public NoticeModel() {
    }

    /**
     * Full constructor for creating a NoticeModel instance.
     *
     * @param noticeId  Unique identifier for the notice.
     * @param title     The heading or title of the notice.
     * @param message   The detailed content of the notice.
     * @param createdBy The role or name of the creator (e.g., "admin").
     * @param timestamp The creation time in milliseconds.
     */
    public NoticeModel(String noticeId, String title, String message, String createdBy, long timestamp) {
        this.noticeId = noticeId;
        this.title = title;
        this.message = message;
        this.createdBy = createdBy;
        this.timestamp = timestamp;
    }

    // Getters and Setters

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
