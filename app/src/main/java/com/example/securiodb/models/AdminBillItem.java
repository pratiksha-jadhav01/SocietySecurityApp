package com.example.securiodb.models;

public class AdminBillItem {
    private String documentId;
    private String userId;
    private String month;
    private String status;

    public AdminBillItem() {}

    public AdminBillItem(String documentId, String userId, String month, String status) {
        this.documentId = documentId;
        this.userId = userId;
        this.month = month;
        this.status = status;
    }

    public String getDocumentId() { return documentId; }
    public String getUserId() { return userId; }
    public String getMonth() { return month; }
    public String getStatus() { return status; }
}
