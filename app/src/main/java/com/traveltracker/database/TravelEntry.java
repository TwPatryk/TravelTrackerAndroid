package com.traveltracker.database;

import java.util.List;

public class TravelEntry {
    private long id;
    private String title;
    private String createdAt;
    private List<Note> notes;
    private List<Photo> photos;
    private List<MapPin> mapPins;
    private List<RouteTrack> routeTracks;
    private List<String> tags;
    private String backgroundPath;
    private String backgroundColor;
    private float backgroundOpacity = 1.0f;
    private String backgroundScaleType = "CENTER_CROP";
    private String itemsBackgroundColor;
    private float itemsBackgroundOpacity = 1.0f;
    private String itemsFontColor;

    // Getters i Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<Note> getNotes() { return notes; }
    public void setNotes(List<Note> notes) { this.notes = notes; }

    public List<Photo> getPhotos() { return photos; }
    public void setPhotos(List<Photo> photos) { this.photos = photos; }

    public List<MapPin> getMapPins() { return mapPins; }
    public void setMapPins(List<MapPin> pins) { this.mapPins = pins; }

    public List<RouteTrack> getRouteTracks() { return routeTracks; }
    public void setRouteTracks(List<RouteTrack> routeTracks) { this.routeTracks = routeTracks; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getBackgroundPath() { return backgroundPath; }
    public void setBackgroundPath(String backgroundPath) { this.backgroundPath = backgroundPath; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public float getBackgroundOpacity() { return backgroundOpacity; }
    public void setBackgroundOpacity(float backgroundOpacity) { this.backgroundOpacity = backgroundOpacity; }

    public String getBackgroundScaleType() { return backgroundScaleType; }
    public void setBackgroundScaleType(String backgroundScaleType) { this.backgroundScaleType = backgroundScaleType; }

    public String getItemsBackgroundColor() { return itemsBackgroundColor; }
    public void setItemsBackgroundColor(String itemsBackgroundColor) { this.itemsBackgroundColor = itemsBackgroundColor; }

    public float getItemsBackgroundOpacity() { return itemsBackgroundOpacity; }
    public void setItemsBackgroundOpacity(float itemsBackgroundOpacity) { this.itemsBackgroundOpacity = itemsBackgroundOpacity; }

    public String getItemsFontColor() { return itemsFontColor; }
    public void setItemsFontColor(String itemsFontColor) { this.itemsFontColor = itemsFontColor; }
}
