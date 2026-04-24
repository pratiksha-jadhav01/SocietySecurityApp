package com.example.securiodb.models;

public class BillItem {
    private String month;
    private String amount;
    private String dueDate;
    private String status;

    public BillItem() {}

    public BillItem(String month, String amount, String dueDate, String status) {
        this.month = month;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    public String getMonth() { return month; }
    public String getAmount() { return amount; }
    public String getDueDate() { return dueDate; }
    public String getStatus() { return status; }
}
