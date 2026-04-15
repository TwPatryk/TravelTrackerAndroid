package com.traveltracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "travel_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // Tabela główna
    private static final String TABLE_ENTRIES = "entries";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CREATED_AT = "created_at";

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabela wpisów
        String createEntriesTable = "CREATE TABLE " + TABLE_ENTRIES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createEntriesTable);

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        onCreate(db);
    }

    // ==================== METODY DLA WPISÓW ====================

    public long insertEntry(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        long id = db.insert(TABLE_ENTRIES, null, values);
        db.close();
        return id;
    }

    public List<TravelEntry> getAllEntries() {
        List<TravelEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_ENTRIES + " ORDER BY " + COLUMN_CREATED_AT + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TravelEntry entry = new TravelEntry();
                entry.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                entry.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));

                // Pobierz notatki dla tego wpisu
                entry.setNotes(getNotesForEntry(entry.getId()));

                // Pobierz zdjęcia dla tego wpisu
                entry.setPhotos(getPhotosForEntry(entry.getId()));

                entries.add(entry);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
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
            entry.setNotes(getNotesForEntry(id));
            entry.setPhotos(getPhotosForEntry(id));
        }
        cursor.close();
        db.close();
        return entry;
    }

    public int updateEntryTitle(long id, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        int result = db.update(TABLE_ENTRIES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    public void deleteEntry(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ENTRIES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ==================== METODY DLA NOTATEK ====================

    public long insertNote(long entryId, String noteText, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ID, entryId);
        values.put(COLUMN_NOTE_TEXT, noteText);
        values.put(COLUMN_NOTE_ORDER, order);
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
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
        db.close();
        return notes;
    }

    public void updateNote(long noteId, String newText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TEXT, newText);
        db.update(TABLE_NOTES, values, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(noteId)});
        db.close();
    }

    public void deleteNote(long noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(noteId)});
        db.close();
    }

    public void deleteAllNotesForEntry(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ENTRY_ID + " = ?", new String[]{String.valueOf(entryId)});
        db.close();
    }

    // ==================== METODY DLA ZDJĘĆ ====================

    public long insertPhoto(long entryId, String photoPath, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_ID, entryId);
        values.put(COLUMN_PHOTO_PATH, photoPath);
        values.put(COLUMN_PHOTO_ORDER, order);
        long id = db.insert(TABLE_PHOTOS, null, values);
        db.close();
        return id;
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
        db.close();
        return photos;
    }

    public void deletePhoto(long photoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Najpierw pobierz ścieżkę pliku
        String query = "SELECT " + COLUMN_PHOTO_PATH + " FROM " + TABLE_PHOTOS + " WHERE " + COLUMN_PHOTO_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(photoId)});
        if (cursor.moveToFirst()) {
            String path = cursor.getString(0);
            // Usuń plik z dysku
            new java.io.File(path).delete();
        }
        cursor.close();

        db.delete(TABLE_PHOTOS, COLUMN_PHOTO_ID + " = ?", new String[]{String.valueOf(photoId)});
        db.close();
    }

    public void deleteAllPhotosForEntry(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Najpierw pobierz wszystkie ścieżki
        String query = "SELECT " + COLUMN_PHOTO_PATH + " FROM " + TABLE_PHOTOS + " WHERE " + COLUMN_ENTRY_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(entryId)});
        if (cursor.moveToFirst()) {
            do {
                String path = cursor.getString(0);
                new java.io.File(path).delete();
            } while (cursor.moveToNext());
        }
        cursor.close();

        db.delete(TABLE_PHOTOS, COLUMN_ENTRY_ID + " = ?", new String[]{String.valueOf(entryId)});
        db.close();
    }
}