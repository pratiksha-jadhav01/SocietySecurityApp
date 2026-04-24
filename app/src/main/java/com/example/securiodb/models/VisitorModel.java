package com.example.securiodb.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

/**
 * Enhanced Visitor model supporting both Firestore and Realtime Database mapping.
 * Handles both Timestamp objects and Long milliseconds for dates.
 */
public class VisitorModel {

    private String docId;
    private String visitorId;
    private String name;
    private String phone;
    private String flatNumber;
    private String purpose;
    private String imageUrl;
    private String photoUrl;
    private String status;
    private String createdBy;
    private String approvedBy;
    private Timestamp entryTime;
    private Timestamp exitTime;
    private Timestamp timestamp;

    public VisitorModel() {}

    @Exclude
    public String getDocId() { return docId != null ? docId : visitorId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getVisitorId() { return visitorId != null ? visitorId : docId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @PropertyName("flatNumber")
    public String getFlatNumber() { return flatNumber; }
    @PropertyName("flatNumber")
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }

    @Exclude
    @PropertyName("flat")
    public String getFlat() { return flatNumber; }
    @PropertyName("flat")
    public void setFlat(String flat) { this.flatNumber = flat; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl != null ? imageUrl : photoUrl; }
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("photoUrl")
    public String getPhotoUrl() { return photoUrl != null ? photoUrl : imageUrl; }
    @PropertyName("photoUrl")
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Timestamp getEntryTime() { return entryTime; }
    public void setEntryTime(Object ts) {
        this.entryTime = convertToTimestamp(ts);
    }

    public Timestamp getExitTime() { return exitTime; }
    public void setExitTime(Object ts) {
        this.exitTime = convertToTimestamp(ts);
    }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Object ts) {
        this.timestamp = convertToTimestamp(ts);
    }

    private Timestamp convertToTimestamp(Object ts) {
        if (ts instanceof Timestamp) return (Timestamp) ts;
        if (ts instanceof Long) return new Timestamp(new Date((Long) ts));
        return null;
    }
}
