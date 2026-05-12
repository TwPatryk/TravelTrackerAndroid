package com.traveltracker;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.traveltracker.database.DatabaseHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final OnBackupListener listener;

    public interface OnBackupListener {
        void onRestoreComplete();
        void onRunOnUiThread(Runnable runnable);
    }

    public BackupManager(Context context, DatabaseHelper dbHelper, OnBackupListener listener) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.listener = listener;
    }

    public void exportAllData(Uri uri) {
        new Thread(() -> {
            boolean success = false;
            try (OutputStream os = context.getContentResolver().openOutputStream(uri);
                 java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(os);
                 ZipOutputStream zos = new ZipOutputStream(bos)) {

                zos.setLevel(java.util.zip.Deflater.BEST_COMPRESSION);

                // 1. Export Database
                File dbFile = context.getDatabasePath("travel_tracker.db");
                if (dbFile.exists()) {
                    addToZip(dbFile, "travel_tracker.db", zos);

                    File walFile = new File(dbFile.getPath() + "-wal");
                    if (walFile.exists()) addToZip(walFile, "travel_tracker.db-wal", zos);

                    File shmFile = new File(dbFile.getPath() + "-shm");
                    if (shmFile.exists()) addToZip(shmFile, "travel_tracker.db-shm", zos);
                }

                // 2. Export Entries folder
                File entriesDir = new File(context.getFilesDir(), "Entries");
                if (entriesDir.exists()) {
                    addFolderToZip(entriesDir, "Entries", zos);
                }

                zos.finish();
                zos.flush();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();
                final String errorMsg = e.getMessage();
                listener.onRunOnUiThread(() -> Toast.makeText(context, "Export failed: " + errorMsg, Toast.LENGTH_LONG).show());
            }

            if (success) {
                listener.onRunOnUiThread(() -> Toast.makeText(context, "Full backup created successfully!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void addToZip(File file, String zipPath, ZipOutputStream zos) throws IOException {
        if (!file.exists() || !file.canRead()) return;

        ZipEntry zipEntry = new ZipEntry(zipPath);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[16384];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
        zos.closeEntry();
    }

    private void addFolderToZip(File folder, String parentPath, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            String path = parentPath + "/" + file.getName();
            if (file.isDirectory()) {
                addFolderToZip(file, path, zos);
            } else {
                addToZip(file, path, zos);
            }
        }
    }

    public void exportDatabase(Uri uri) {
        File dbFile = context.getDatabasePath("travel_tracker.db");
        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            Toast.makeText(context, "Database exported successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void importDatabase(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName != null && fileName.endsWith(".zip")) {
            importFullBackup(uri);
            return;
        }

        dbHelper.close();
        new Thread(() -> {
            boolean success = false;
            File dbFile = context.getDatabasePath("travel_tracker.db");
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(dbFile)) {
                byte[] buf = new byte[16384];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                success = true;
            } catch (IOException e) {
                e.printStackTrace();
                final String msg = e.getMessage();
                listener.onRunOnUiThread(() -> Toast.makeText(context, "Import failed: " + msg, Toast.LENGTH_LONG).show());
            }

            if (success) {
                listener.onRunOnUiThread(() -> {
                    Toast.makeText(context, "Database imported successfully.", Toast.LENGTH_SHORT).show();
                    listener.onRestoreComplete();
                });
            }
        }).start();
    }

    public String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    public void importFullBackup(Uri uri) {
        dbHelper.close();
        new Thread(() -> {
            boolean success = false;
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 ZipInputStream zis = new ZipInputStream(is)) {

                // Clear existing entries for a clean restore
                File entriesDir = new File(context.getFilesDir(), "Entries");
                if (entriesDir.exists()) {
                    deleteRecursive(entriesDir);
                }

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File outFile;
                    String name = entry.getName();

                    if (name.equals("travel_tracker.db")) {
                        outFile = context.getDatabasePath("travel_tracker.db");
                    } else if (name.equals("travel_tracker.db-wal")) {
                        outFile = context.getDatabasePath("travel_tracker.db-wal");
                    } else if (name.equals("travel_tracker.db-shm")) {
                        outFile = context.getDatabasePath("travel_tracker.db-shm");
                    } else {
                        outFile = new File(context.getFilesDir(), name);
                    }

                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        File parent = outFile.getParentFile();
                        if (parent != null) parent.mkdirs();

                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[16384];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
                success = true;
            } catch (IOException e) {
                e.printStackTrace();
                final String msg = e.getMessage();
                listener.onRunOnUiThread(() -> Toast.makeText(context, "Restore failed: " + msg, Toast.LENGTH_LONG).show());
            }

            if (success) {
                listener.onRunOnUiThread(() -> {
                    Toast.makeText(context, "Full backup restored successfully!", Toast.LENGTH_SHORT).show();
                    listener.onRestoreComplete();
                });
            }
        }).start();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}
