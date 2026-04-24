package com.example.securiodb.models;

import com.google.firebase.Timestamp;

public class Bill {
    private String billId;
    private String flatNo;
    private String month;
    private double amount;
    private String dueDate;
    private String status; // "due" or "paid"
    private Timestamp timestamp;

    public Bill() {
        // Required for Firestore
    }

    public Bill(String billId, String flatNo, String month, double amount, String dueDate, String status, Timestamp timestamp) {
        this.billId = billId;
        this.flatNo = flatNo;
        this.month = month;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public String getFlatNo() { return flatNo; }
    public void setFlatNo(String flatNo) { this.flatNo = flatNo; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
