package com.traveltracker.database;

public class MapPin extends EntryItem {
    private long id;
    private long entryId;
    private String label;
    private double latitude;
    private double longitude;
    private int order;

    // Getters i Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getEntryId() { return entryId; }
    public void setEntryId(long entryId) { this.entryId = entryId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @Override
    public int getOrder() { return order; }
    @Override
    public void setOrder(int order) { this.order = order; }

    @Override
    public boolean isNote() { return false; }
    
    @Override
    public boolean isMapPin() { return true; }
}
