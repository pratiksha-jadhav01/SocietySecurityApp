package com.example.securiodb.models;

import com.google.firebase.database.PropertyName;

/**
 * Model class for Complaints.
 * Updated to support both Realtime Database and Firestore fields.
 */
public class ComplaintModel {
    private String complaintId;
    private String userId;
    private String ownerUid; // For Firestore
    private String title;
    private String description;
    private String status;
    private String response;
    private String adminResponse; // For consistency with some parts of the app
    private long timestamp;
    private long raisedOn; // For Firestore
    private String category;
    private String imageUrl;
    private String flatNo;
    private String resolutionImageUrl;

    public ComplaintModel() {}

    // Getters
    public String getComplaintId()  { return complaintId; }
    public String getUserId()       { return userId != null ? userId : ownerUid; }
    public String getOwnerUid()     { return ownerUid != null ? ownerUid : userId; }
    public String getTitle()        { return title; }
    public String getDescription()  { return description; }
    public String getStatus()       { return status; }
    public String getResponse()     { return response != null ? response : adminResponse; }
    public String getAdminResponse(){ return adminResponse != null ? adminResponse : response; }
    public long   getTimestamp()    { return timestamp != 0 ? timestamp : raisedOn; }
    public long   getRaisedOn()     { return raisedOn != 0 ? raisedOn : timestamp; }
    public String getCategory()     { return category; }
    public String getImageUrl()     { return imageUrl; }
    public String getFlatNo()       { return flatNo; }
    public String getResolutionImageUrl() { return resolutionImageUrl; }

    // Setters
    public void setComplaintId(String id)  { this.complaintId = id; }
    public void setUserId(String userId)   { this.userId = userId; }
    public void setOwnerUid(String ownerUid){ this.ownerUid = ownerUid; }
    public void setTitle(String title)     { this.title = title; }
    public void setDescription(String d)   { this.description = d; }
    public void setStatus(String status)   { this.status = status; }
    public void setResponse(String r)      { this.response = r; }
    public void setAdminResponse(String r) { this.adminResponse = r; }
    public void setTimestamp(long t)       { this.timestamp = t; }
    public void setRaisedOn(long t)        { this.raisedOn = t; }
    public void setCategory(String c)      { this.category = c; }
    public void setImageUrl(String i)      { this.imageUrl = i; }
    public void setFlatNo(String flatNo)   { this.flatNo = flatNo; }
    public void setResolutionImageUrl(String url) { this.resolutionImageUrl = url; }
}
