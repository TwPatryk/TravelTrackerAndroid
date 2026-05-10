package com.traveltracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;

    private static final String DATABASE_NAME = "travel_tracker.db";
    private static final int DATABASE_VERSION = 13;

    // Tabela główna
    private static final String TABLE_ENTRIES = "entries";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_ENTRY_ORDER = "entry_order";
    private static final String COLUMN_BG_PATH = "bg_path";
    private static final String COLUMN_BG_COLOR = "bg_color";
    private static final String COLUMN_BG_OPACITY = "bg_opacity";
    private static final String COLUMN_BG_SCALE_TYPE = "bg_scale_type";
    private static final String COLUMN_BG_OFFSET_X = "bg_offset_x";
    private static final String COLUMN_BG_OFFSET_Y = "bg_offset_y";
    private static final String COLUMN_ITEMS_BG_COLOR = "item_bg_color";
    private static final String COLUMN_ITEMS_BG_OPACITY = "item_bg_opacity";
    private static final String COLUMN_ITEMS_FONT_COLOR = "item_font_color";

    // Tabela ustawień globalnych
    private static final String TABLE_SETTINGS = "settings";
    private static final String COLUMN_SETTING_KEY = "setting_key";
    private static final String COLUMN_SETTING_VALUE = "setting_value";
    private static final String TABLE_TAGS = "tags";
    private static final String COLUMN_TAG_ID = "id";
    private static final String COLUMN_TAG_NAME = "tag_name";

    // Tabela notatek
    private static final String TABLE_NOTES = "notes";
    private static final String COLUMN_NOTE_ID = "id";
    private static final String COLUMN_ENTRY_ID = "entry_id";
    private static final String COLUMN_NOTE_TEXT = "note_text";
    private static final String COLUMN_NOTE_ORDER = "note_order";

    // Tabela zdjęć
    private static final String TABLE_PHOTOS = "photos";
    private static final String COLUMN_PHOTO_ID = "id";
    private static final String COLUMN_PHOTO_PATH = "photo_path";
    private static final String COLUMN_PHOTO_ORDER = "photo_order";

    // Tabela pinezek mapy
    private static final String TABLE_PINS = "map_pins";
    private static final String COLUMN_PIN_ID = "id";
    private static final String COLUMN_PIN_LABEL = "label";
    private static final String COLUMN_PIN_LAT = "latitude";
    private static final String COLUMN_PIN_LON = "longitude";
    private static final String COLUMN_PIN_ORDER = "pin_order";

    // Tabela tras (tracków)
    private static final String TABLE_ROUTE_TRACKS = "route_tracks";
    private static final String COLUMN_TRACK_ID = "id";
    private static final String COLUMN_TRACK_NAME = "name";
    private static final String COLUMN_TRACK_FILE_PATH = "file_path";
    private static final String COLUMN_TRACK_ORDER = "track_order";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabela wpisów
        String createEntriesTable = "CREATE TABLE " + TABLE_ENTRIES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP, " +
                COLUMN_ENTRY_ORDER + " INTEGER DEFAULT 0, " +
                COLUMN_BG_PATH + " TEXT, " +
                COLUMN_BG_COLOR + " TEXT, " +
                COLUMN_BG_OPACITY + " REAL DEFAULT 1.0, " +
                COLUMN_BG_SCALE_TYPE + " TEXT DEFAULT 'CENTER_CROP', " +
                COLUMN_BG_OFFSET_X + " REAL DEFAULT 0.5, " +
                COLUMN_BG_OFFSET_Y + " REAL DEFAULT 0.5, " +
                COLUMN_ITEMS_BG_COLOR + " TEXT, " +
                COLUMN_ITEMS_BG_OPACITY + " REAL DEFAULT 1.0, " +
                COLUMN_ITEMS_FONT_COLOR + " TEXT)";
        db.execSQL(createEntriesTable);

        // Tabela ustawień
        String createSettingsTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, " +
                COLUMN_SETTING_VALUE + " TEXT)";
        db.execSQL(createSettingsTable);

        // Tabela notatek
        String createNotesTable = "CREATE TABLE " + TABLE_NOTES + " (" +
                COLUMN_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ENTRY_ID + " INTEGER, " +
                COLUMN_NOTE_TEXT + " TEXT, " +
                COLUMN_NOTE_ORDER + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)";
        db.execSQL(createNotesTable);

        // Tabela zdjęć
        String createPhotosTable = "CREATE TABLE " + TABLE_PHOTOS + " (" +
                COLUMN_PHOTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ENTRY_ID + " INTEGER, " +
                COLUMN_PHOTO_PATH + " TEXT, " +
                COLUMN_PHOTO_ORDER + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)";
        db.execSQL(createPhotosTable);

        // Tabela pinezek
        String createPinsTable = "CREATE TABLE " + TABLE_PINS + " (" +
                COLUMN_PIN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ENTRY_ID + " INTEGER, " +
                COLUMN_PIN_LABEL + " TEXT, " +
                COLUMN_PIN_LAT + " REAL, " +
                COLUMN_PIN_LON + " REAL, " +
                COLUMN_PIN_ORDER + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)";
        db.execSQL(createPinsTable);

        // Tabela tagów
        String createTagsTable = "CREATE TABLE " + TABLE_TAGS + " (" +
                COLUMN_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ENTRY_ID + " INTEGER, " +
                COLUMN_TAG_NAME + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)";
        db.execSQL(createTagsTable);

        // Tabela tras
        String createRouteTracksTable = "CREATE TABLE " + TABLE_ROUTE_TRACKS + " (" +
                COLUMN_TRACK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ENTRY_ID + " INTEGER, " +
                COLUMN_TRACK_NAME + " TEXT, " +
                COLUMN_TRACK_FILE_PATH + " TEXT, " +
                COLUMN_TRACK_ORDER + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)";
        db.execSQL(createRouteTracksTable);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 7) {
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_ENTRY_ORDER + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_BG_PATH + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_BG_OPACITY + " REAL DEFAULT 1.0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_BG_SCALE_TYPE + " TEXT DEFAULT 'CENTER_CROP'"); } catch (Exception ignored) {}
            
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " (" +
                    COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, " +
                    COLUMN_SETTING_VALUE + " TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TAGS + " (" +
                    COLUMN_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ENTRY_ID + " INTEGER, " +
                    COLUMN_TAG_NAME + " TEXT, " +
                    "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)");
        }

        if (oldVersion < 8) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " (" +
                    COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, " +
                    COLUMN_SETTING_VALUE + " TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TAGS + " (" +
                    COLUMN_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ENTRY_ID + " INTEGER, " +
                    COLUMN_TAG_NAME + " TEXT, " +
                    "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)");
        }
        
        if (oldVersion < 10) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ROUTE_TRACKS + " (" +
                    COLUMN_TRACK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ENTRY_ID + " INTEGER, " +
                    COLUMN_TRACK_NAME + " TEXT, " +
                    COLUMN_TRACK_FILE_PATH + " TEXT, " +
                    COLUMN_TRACK_ORDER + " INTEGER, " +
                    "FOREIGN KEY(" + COLUMN_ENTRY_ID + ") REFERENCES " + TABLE_ENTRIES + "(" + COLUMN_ID + ") ON DELETE CASCADE)");
        }

        if (oldVersion < 11) {
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_ITEMS_BG_COLOR + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_ITEMS_BG_OPACITY + " REAL DEFAULT 1.0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_ITEMS_FONT_COLOR + " TEXT"); } catch (Exception ignored) {}
        }

        if (oldVersion < 12) {
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_BG_OFFSET_X + " REAL DEFAULT 0.5"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_BG_OFFSET_Y + " REAL DEFAULT 0.5"); } catch (Exception ignored) {}
        }
        if (oldVersion < 13) {
             try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_BG_SCALE_TYPE + " TEXT DEFAULT 'CENTER_CROP'"); } catch (Exception ignored) {}
             try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_ITEMS_BG_COLOR + " TEXT"); } catch (Exception ignored) {}
             try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_ITEMS_BG_OPACITY + " REAL DEFAULT 1.0"); } catch (Exception ignored) {}
             try { db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_ITEMS_FONT_COLOR + " TEXT"); } catch (Exception ignored) {}
        }
    }

    // ==================== METODY DLA WPISÓW ====================

    public long insertEntry(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        return db.insert(TABLE_ENTRIES, null, values);
    }

    public List<TravelEntry> getAllEntries() {
        List<TravelEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_ENTRIES + " ORDER BY " + COLUMN_ENTRY_ORDER + " ASC, " + COLUMN_CREATED_AT + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TravelEntry entry = new TravelEntry();
                entry.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                entry.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
                entry.setBackgroundPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BG_PATH)));
                entry.setBackgroundColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BG_COLOR)));
                entry.setBackgroundOpacity(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_BG_OPACITY)));
                entry.setBackgroundScaleType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BG_SCALE_TYPE)));
                entry.setBackgroundOffsetX(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_BG_OFFSET_X)));
                entry.setBackgroundOffsetY(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_BG_OFFSET_Y)));
                entry.setItemsBackgroundColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_BG_COLOR)));
                entry.setItemsBackgroundOpacity(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_BG_OPACITY)));
                entry.setItemsFontColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_FONT_COLOR)));

                entry.setNotes(getNotesForEntry(entry.getId()));
                entry.setPhotos(getPhotosForEntry(entry.getId()));
                entry.setMapPins(getMapPinsForEntry(entry.getId()));
                entry.setRouteTracks(getRouteTracksForEntry(entry.getId()));
                entry.setTags(getTagsForEntry(entry.getId()));

                entries.add(entry);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }

    public TravelEntry getEntryById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        TravelEntry entry = null;

        String query = "SELECT * FROM " + TABLE_ENTRIES + " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            entry = new TravelEntry();
            entry.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
            entry.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
            entry.setBackgroundPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BG_PATH)));
            entry.setBackgroundColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BG_COLOR)));
            entry.setBackgroundOpacity(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_BG_OPACITY)));
            entry.setBackgroundScaleType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BG_SCALE_TYPE)));
            entry.setBackgroundOffsetX(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_BG_OFFSET_X)));
            entry.setBackgroundOffsetY(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_BG_OFFSET_Y)));
            entry.setItemsBackgroundColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_BG_COLOR)));
            entry.setItemsBackgroundOpacity(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_BG_OPACITY)));
            entry.setItemsFontColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_FONT_COLOR)));
            entry.setNotes(getNotesForEntry(id));
            entry.setPhotos(getPhotosForEntry(id));
            entry.setMapPins(getMapPinsForEntry(id));
            entry.setRouteTracks(getRouteTracksForEntry(id));
            entry.setTags(getTagsForEntry(id));
        }
        cursor.close();
        return entry;
    }

    public int updateEntryTitle(long id, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        return db.update(TABLE_ENTRIES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteEntry(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ENTRIES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void updateEntryOrder(long id, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ORDER, order);
        db.update(TABLE_ENTRIES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ==================== METODY DLA NOTATEK ====================

    public long insertNote(long entryId, String noteText, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ID, entryId);
        values.put(COLUMN_NOTE_TEXT, noteText);
        values.put(COLUMN_NOTE_ORDER, order);
        return db.insert(TABLE_NOTES, null, values);
    }

    public List<Note> getNotesForEntry(long entryId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_NOTES + " WHERE " + COLUMN_ENTRY_ID + " = ? ORDER BY " + COLUMN_NOTE_ORDER;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(entryId)});

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID)));
                note.setEntryId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENTRY_ID)));
                note.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TEXT)));
                note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ORDER)));
                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }

    public void updateNote(long noteId, String newText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TEXT, newText);
        db.update(TABLE_NOTES, values, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(noteId)});
    }

    public void deleteNote(long noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(noteId)});
    }

    public void deleteAllNotesForEntry(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ENTRY_ID + " = ?", new String[]{String.valueOf(entryId)});
    }

    // ==================== METODY DLA ZDJĘĆ ====================

    public long insertPhoto(long entryId, String photoPath, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ID, entryId);
        values.put(COLUMN_PHOTO_PATH, photoPath);
        values.put(COLUMN_PHOTO_ORDER, order);
        return db.insert(TABLE_PHOTOS, null, values);
    }

    public List<Photo> getPhotosForEntry(long entryId) {
        List<Photo> photos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_PHOTOS + " WHERE " + COLUMN_ENTRY_ID + " = ? ORDER BY " + COLUMN_PHOTO_ORDER;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(entryId)});

        if (cursor.moveToFirst()) {
            do {
                Photo photo = new Photo();
                photo.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ID)));
                photo.setEntryId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENTRY_ID)));
                photo.setPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH)));
                photo.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ORDER)));
                photos.add(photo);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return photos;
    }

    public void deletePhoto(long photoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PHOTOS, COLUMN_PHOTO_ID + " = ?", new String[]{String.valueOf(photoId)});
    }

    public void deleteAllPhotosForEntry(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PHOTOS, COLUMN_ENTRY_ID + " = ?", new String[]{String.valueOf(entryId)});
    }

    // ==================== METODY DLA PINEZEK ====================

    public long insertMapPin(long entryId, String label, double lat, double lon, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ID, entryId);
        values.put(COLUMN_PIN_LABEL, label);
        values.put(COLUMN_PIN_LAT, lat);
        values.put(COLUMN_PIN_LON, lon);
        values.put(COLUMN_PIN_ORDER, order);
        return db.insert(TABLE_PINS, null, values);
    }

    public List<MapPin> getMapPinsForEntry(long entryId) {
        List<MapPin> pins = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_PINS + " WHERE " + COLUMN_ENTRY_ID + " = ? ORDER BY " + COLUMN_PIN_ORDER;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(entryId)});

        if (cursor.moveToFirst()) {
            do {
                MapPin pin = new MapPin();
                pin.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PIN_ID)));
                pin.setEntryId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENTRY_ID)));
                pin.setLabel(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PIN_LABEL)));
                pin.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PIN_LAT)));
                pin.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PIN_LON)));
                pin.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PIN_ORDER)));
                pins.add(pin);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return pins;
    }

    public void deleteAllPinsForEntry(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PINS, COLUMN_ENTRY_ID + " = ?", new String[]{String.valueOf(entryId)});
    }

    // ==================== METODY DLA TAGÓW ====================

    public void insertTag(long entryId, String tagName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ID, entryId);
        values.put(COLUMN_TAG_NAME, tagName);
        db.insert(TABLE_TAGS, null, values);
    }

    public List<String> getTagsForEntry(long entryId) {
        List<String> tags = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_TAG_NAME + " FROM " + TABLE_TAGS + " WHERE " + COLUMN_ENTRY_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(entryId)});
        if (cursor.moveToFirst()) {
            do {
                tags.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tags;
    }

    public List<String> getAllUniqueTags() {
        List<String> tags = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT DISTINCT " + COLUMN_TAG_NAME + " FROM " + TABLE_TAGS + " ORDER BY " + COLUMN_TAG_NAME + " ASC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                tags.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tags;
    }

    public void updateEntryBackground(long id, String path, String color, float opacity, String scaleType, float offsetX, float offsetY, String itemsBg, float itemsOpacity, String itemsFont) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BG_PATH, path);
        values.put(COLUMN_BG_COLOR, color);
        values.put(COLUMN_BG_OPACITY, opacity);
        values.put(COLUMN_BG_SCALE_TYPE, scaleType);
        values.put(COLUMN_BG_OFFSET_X, offsetX);
        values.put(COLUMN_BG_OFFSET_Y, offsetY);
        values.put(COLUMN_ITEMS_BG_COLOR, itemsBg);
        values.put(COLUMN_ITEMS_BG_OPACITY, itemsOpacity);
        values.put(COLUMN_ITEMS_FONT_COLOR, itemsFont);
        db.update(TABLE_ENTRIES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void setGlobalSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_KEY, key);
        values.put(COLUMN_SETTING_VALUE, value);
        db.replace(TABLE_SETTINGS, null, values);
    }

    public String getGlobalSetting(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        String value = null;
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COLUMN_SETTING_VALUE}, COLUMN_SETTING_KEY + " = ?", new String[]{key}, null, null, null);
        if (cursor.moveToFirst()) {
            value = cursor.getString(0);
        }
        cursor.close();
        return value;
    }

    public void deleteAllTagsForEntry(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TAGS, COLUMN_ENTRY_ID + " = ?", new String[]{String.valueOf(entryId)});
    }

    // ==================== METODY DLA TRAS (ROUTE TRACKS) ====================

    public long insertRouteTrack(long entryId, String name, String filePath, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ID, entryId);
        values.put(COLUMN_TRACK_NAME, name);
        values.put(COLUMN_TRACK_FILE_PATH, filePath);
        values.put(COLUMN_TRACK_ORDER, order);
        return db.insert(TABLE_ROUTE_TRACKS, null, values);
    }

    public List<RouteTrack> getRouteTracksForEntry(long entryId) {
        List<RouteTrack> tracks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_ROUTE_TRACKS + " WHERE " + COLUMN_ENTRY_ID + " = ? ORDER BY " + COLUMN_TRACK_ORDER;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(entryId)});

        if (cursor.moveToFirst()) {
            do {
                RouteTrack track = new RouteTrack();
                track.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TRACK_ID)));
                track.setEntryId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENTRY_ID)));
                track.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRACK_NAME)));
                track.setFilePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRACK_FILE_PATH)));
                track.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TRACK_ORDER)));
                tracks.add(track);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tracks;
    }

    public void deleteAllRouteTracksForEntry(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ROUTE_TRACKS, COLUMN_ENTRY_ID + " = ?", new String[]{String.valueOf(entryId)});
    }
}
