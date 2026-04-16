package com.traveltracker.database;

public abstract class EntryItem {
    public abstract int getOrder();
    public abstract void setOrder(int order);
    public abstract boolean isNote();
    public boolean isPhoto() { return false; }
    public boolean isMapPin() { return false; }
}
