package com.example.securiodb.models;

public class Flat {
    private String flatId;
    private String flatNumber;
    private String ownerName;
    private String ownerId;
    private boolean isActive;
    private String floor;

    public Flat() {}

    public Flat(String flatId, String flatNumber, String ownerName, String ownerId, boolean isActive, String floor) {
        this.flatId = flatId;
        this.flatNumber = flatNumber;
        this.ownerName = ownerName;
        this.ownerId = ownerId;
        this.isActive = isActive;
        this.floor = floor;
    }

    public String getFlatId() { return flatId; }
    public String getFlatNumber() { return flatNumber; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerId() { return ownerId; }
    public boolean isActive() { return isActive; }
    public String getFloor() { return floor; }

    public void setFlatId(String flatId) { this.flatId = flatId; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setActive(boolean active) { isActive = active; }
    public void setFloor(String floor) { this.floor = floor; }
}
