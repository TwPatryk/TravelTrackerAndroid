package com.traveltracker.database;

public class Note extends EntryItem {
    private long id;
    private long entryId;
    private String text;
    private int order;

    // Getters i Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getEntryId() { return entryId; }
    public void setEntryId(long entryId) { this.entryId = entryId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    @Override
    public int getOrder() { return order; }
    @Override
    public void setOrder(int order) { this.order = order; }

    @Override
    public boolean isNote() { return true; }
}
