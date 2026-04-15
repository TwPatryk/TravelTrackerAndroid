package com.traveltracker.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {

    public static String saveImageToInternalStorage(Context context, Uri imageUri, String entryId) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

            // Skompresuj obrazek
            bitmap = compressBitmap(bitmap, 1024);

            // Utwórz katalog dla aplikacji
            File directory = new File(context.getFilesDir(), "travel_photos");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generuj unikalną nazwę pliku
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "photo_" + entryId + "_" + timeStamp + ".jpg";
            File file = new File(directory, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();

            return file.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap compressBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > maxSize || height > maxSize) {
            float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
            int newWidth = Math.round(width * ratio);
            int newHeight = Math.round(height * ratio);
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        return bitmap;
    }

    public static void deleteImage(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}