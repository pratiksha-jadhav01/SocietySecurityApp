package com.example.securiodb.models;

/**
 * Model for Maintenance Bill used in History.
 */
public class MaintenanceBillModel {
    private String month;
    private double amount;
    private String dueDate;
    private String status; // "Paid" or "Unpaid"

    public MaintenanceBillModel() {}

    public MaintenanceBillModel(String month, double amount, String dueDate, String status) {
        this.month = month;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
