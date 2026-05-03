package com.traveltracker.database;

public class RouteTrack extends EntryItem {
    private long id;
    private long entryId;
    private String name;
    private String filePath;
    private int order;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getEntryId() { return entryId; }
    public void setEntryId(long entryId) { this.entryId = entryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public int getOrder() { return order; }
    @Override
    public void setOrder(int order) { this.order = order; }

    @Override
    public boolean isNote() { return false; }
    
    public boolean isRouteTrack() { return true; }
}
