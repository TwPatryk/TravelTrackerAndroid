package com.traveltracker.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.traveltracker.R;
import com.traveltracker.database.DatabaseHelper;
import com.traveltracker.database.EntryItem;
import com.traveltracker.database.MapPin;
import com.traveltracker.database.Note;
import com.traveltracker.database.Photo;
import com.traveltracker.database.RouteTrack;
import com.traveltracker.database.TravelEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AddEditEntryFragment extends DialogFragment {

    private static final String ARG_ENTRY_ID = "entry_id";

    private TravelEntry currentEntry;
    private DatabaseHelper dbHelper;
    private OnEntrySavedListener onEntrySavedListener;

    private EditText titleInput, tagsInput;
    private RecyclerView itemsRecyclerView;
    private ItemsAdapter itemsAdapter;
    private ItemTouchHelper itemTouchHelper;
    private ImageView backgroundImageView;
    private View backgroundOverlay;

    private List<EntryItem> itemsList = new ArrayList<>();

    private final ActivityResultLauncher<String[]> backgroundPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (currentEntry != null) {
                        currentEntry.setBackgroundPath(uri.toString());
                        applyBackgroundSettings();
                    }
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted || coarseLocationGranted != null && coarseLocationGranted) {
                    addNewPinWithCurrentLocation();
                } else {
                    Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveImageToInternalStorage(uri);
                }
            });

    private final ActivityResultLauncher<String[]> trackPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    saveTrackToInternalStorage(uri);
                }
            });

    private void saveTrackToInternalStorage(Uri uri) {
        try {
            String fileName = "TRACK_" + System.currentTimeMillis() + ".gpx";
            String title = titleInput.getText().toString().trim();
            File targetDir = title.isEmpty() ? requireContext().getFilesDir() : getEntryDirectory(title);
            File file = new File(targetDir, fileName);

            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) return;
            FileOutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();

            RouteTrack newTrack = new RouteTrack();
            newTrack.setName("New Track");
            newTrack.setFilePath(file.getAbsolutePath());
            newTrack.setOrder(itemsList.size());
            itemsList.add(newTrack);
            itemsAdapter.notifyItemInserted(itemsList.size() - 1);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving track", Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnEntrySavedListener {
        void onEntrySaved();
    }

    public static AddEditEntryFragment newInstance(TravelEntry entry) {
        AddEditEntryFragment fragment = new AddEditEntryFragment();
        if (entry != null) {
            Bundle args = new Bundle();
            args.putLong(ARG_ENTRY_ID, entry.getId());
            fragment.setArguments(args);
        }
        return fragment;
    }

    public void setOnEntrySavedListener(OnEntrySavedListener listener) {
        this.onEntrySavedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        dbHelper = DatabaseHelper.getInstance(getContext());
        if (getArguments() != null) {
            long entryId = getArguments().getLong(ARG_ENTRY_ID);
            currentEntry = dbHelper.getEntryById(entryId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_edit_entry, container, false);

        titleInput = view.findViewById(R.id.entry_title_input);
        tagsInput = view.findViewById(R.id.entry_tags_input);
        itemsRecyclerView = view.findViewById(R.id.items_recycler_view);
        backgroundImageView = view.findViewById(R.id.entry_background_image);
        backgroundOverlay = view.findViewById(R.id.entry_background_overlay);

        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        itemsAdapter = new ItemsAdapter();
        itemsRecyclerView.setAdapter(itemsAdapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(itemsList, fromPos, toPos);
                itemsAdapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                itemsAdapter.notifyDataSetChanged(); // Ensure items are properly updated
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(itemsRecyclerView);

        Button btnAddNote = view.findViewById(R.id.btn_add_note);
        Button btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        Button btnAddPin = view.findViewById(R.id.btn_add_pin);
        Button btnAddTrack = view.findViewById(R.id.btn_add_track);
        Button btnSave = view.findViewById(R.id.btn_save_entry);
        Button btnCancel = view.findViewById(R.id.btn_cancel_entry);
        Button btnEditBg = view.findViewById(R.id.btn_edit_background);

        // Apply theme color to buttons
        applyThemeToUI(view);

        if (currentEntry != null) {
            titleInput.setText(currentEntry.getTitle());
            if (currentEntry.getTags() != null && !currentEntry.getTags().isEmpty()) {
                tagsInput.setText(String.join(", ", currentEntry.getTags()));
            }
            itemsList.clear();
            if (currentEntry.getNotes() != null) itemsList.addAll(currentEntry.getNotes());
            if (currentEntry.getPhotos() != null) itemsList.addAll(currentEntry.getPhotos());
            if (currentEntry.getMapPins() != null) itemsList.addAll(currentEntry.getMapPins());
            if (currentEntry.getRouteTracks() != null) itemsList.addAll(currentEntry.getRouteTracks());
            
            Collections.sort(itemsList, Comparator.comparingInt(EntryItem::getOrder));
            itemsAdapter.notifyDataSetChanged();
            applyBackgroundSettings();
        }

        btnAddNote.setOnClickListener(v -> {
            Note newNote = new Note();
            newNote.setText("");
            newNote.setOrder(itemsList.size());
            itemsList.add(newNote);
            itemsAdapter.notifyItemInserted(itemsList.size() - 1);
        });

        btnAddPhoto.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });

        btnAddPin.setOnClickListener(v -> {
            showAddPinOptions();
        });

        btnAddTrack.setOnClickListener(v -> {
            trackPickerLauncher.launch(new String[]{"*/*"});
        });

        btnEditBg.setOnClickListener(v -> {
            if (currentEntry == null) {
                Toast.makeText(getContext(), "Please save the entry first to set a background", Toast.LENGTH_SHORT).show();
                return;
            }
            showBackgroundSettingsDialog();
        });

        btnSave.setOnClickListener(v -> saveEntry());
        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }

    private int getThemeColor() {
        if (getContext() == null) return android.graphics.Color.parseColor("#FFFF3D00");
        int primaryColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary);
        String themeColorHex = dbHelper.getGlobalSetting("theme_color");
        String fabColorHex = dbHelper.getGlobalSetting("fab_bg_color");
        
        String colorHex = (fabColorHex != null && !fabColorHex.isEmpty()) ? fabColorHex : themeColorHex;
        
        if (colorHex == null || colorHex.isEmpty()) return primaryColor;
        try {
            return android.graphics.Color.parseColor(colorHex);
        } catch (Exception e) {
            return primaryColor;
        }
    }

    private void applyThemeToUI(View view) {
        int themeColor = getThemeColor();
        android.content.res.ColorStateList themeTint = android.content.res.ColorStateList.valueOf(themeColor);
        android.content.res.ColorStateList whiteTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE);

        com.google.android.material.button.MaterialButton btnAddNote = view.findViewById(R.id.btn_add_note);
        com.google.android.material.button.MaterialButton btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        com.google.android.material.button.MaterialButton btnAddPin = view.findViewById(R.id.btn_add_pin);
        com.google.android.material.button.MaterialButton btnAddTrack = view.findViewById(R.id.btn_add_track);
        Button btnSave = view.findViewById(R.id.btn_save_entry);
        com.google.android.material.button.MaterialButton btnEditBg = view.findViewById(R.id.btn_edit_background);

        if (btnAddNote != null) {
            btnAddNote.setBackgroundTintList(themeTint);
            btnAddNote.setTextColor(whiteTint);
            btnAddNote.setIconTint(whiteTint);
        }
        if (btnAddPhoto != null) {
            btnAddPhoto.setBackgroundTintList(themeTint);
            btnAddPhoto.setTextColor(whiteTint);
            btnAddPhoto.setIconTint(whiteTint);
        }
        if (btnAddPin != null) {
            btnAddPin.setBackgroundTintList(themeTint);
            btnAddPin.setTextColor(whiteTint);
            btnAddPin.setIconTint(whiteTint);
        }
        if (btnAddTrack != null) {
            btnAddTrack.setBackgroundTintList(themeTint);
            btnAddTrack.setTextColor(whiteTint);
            btnAddTrack.setIconTint(whiteTint);
        }
        if (btnSave != null) {
            btnSave.setBackgroundTintList(themeTint);
            btnSave.setTextColor(android.graphics.Color.WHITE);
        }
        if (btnEditBg != null) {
            btnEditBg.setBackgroundTintList(themeTint);
            btnEditBg.setTextColor(whiteTint);
            btnEditBg.setIconTint(whiteTint);
        }
    }

    private void showAddPinOptions() {
        String[] options = {"Current Location", "Enter Coordinates"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Map Pin")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkLocationPermissionAndAddPin();
                    } else {
                        showCustomLocationDialog();
                    }
                })
                .show();
    }

    private void showCustomLocationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_location, null);
        EditText latInput = dialogView.findViewById(R.id.lat_input);
        EditText lonInput = dialogView.findViewById(R.id.lon_input);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Enter Coordinates")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    try {
                        String latStr = latInput.getText().toString();
                        String lonStr = lonInput.getText().toString();

                        if (latStr.isEmpty() || lonStr.isEmpty()) {
                            Toast.makeText(getContext(), "Please enter both coordinates", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double lat = Double.parseDouble(latStr);
                        double lon = Double.parseDouble(lonStr);

                        MapPin newPin = new MapPin();
                        newPin.setLabel("");
                        newPin.setLatitude(lat);
                        newPin.setLongitude(lon);
                        newPin.setOrder(itemsList.size());
                        itemsList.add(newPin);
                        itemsAdapter.notifyItemInserted(itemsList.size() - 1);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid coordinates format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkLocationPermissionAndAddPin() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            addNewPinWithCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void addNewPinWithCurrentLocation() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                Location lastKnownLocation = null;
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                MapPin newPin = new MapPin();
                newPin.setLabel("");
                if (lastKnownLocation != null) {
                    newPin.setLatitude(lastKnownLocation.getLatitude());
                    newPin.setLongitude(lastKnownLocation.getLongitude());
                } else {
                    // Fallback if no location found
                    newPin.setLatitude(52.2297);
                    newPin.setLongitude(21.0122);
                    Toast.makeText(getContext(), "Could not get current location, using default", Toast.LENGTH_SHORT).show();
                }
                newPin.setOrder(itemsList.size());
                itemsList.add(newPin);
                itemsAdapter.notifyItemInserted(itemsList.size() - 1);
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Permission error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            // 1. Get image dimensions without loading into memory
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            // 2. Calculate optimal sample size to avoid OOM
            int maxWidth = 2500;
            int maxHeight = 2500;
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
            options.inJustDecodeBounds = false;

            // 3. Decode with sample size
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
                String title = titleInput.getText().toString().trim();
                File targetDir = title.isEmpty() ? requireContext().getFilesDir() : getEntryDirectory(title);
                
                File file = new File(targetDir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    out.flush();
                }

                Photo newPhoto = new Photo();
                newPhoto.setPath(file.getAbsolutePath());
                newPhoto.setOrder(itemsList.size());
                itemsList.add(newPhoto);
                itemsAdapter.notifyItemInserted(itemsList.size() - 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void showFullScreenImage(String path) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);
        ImageView fullScreenImage = dialog.findViewById(R.id.full_screen_image);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_full_screen);
        
        File imgFile = new File(path);
        Glide.with(this)
                .load(imgFile.exists() ? imgFile : path)
                .fitCenter()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(fullScreenImage);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void applyBackgroundSettings() {
        if (currentEntry == null || backgroundImageView == null) return;
        
        String path = currentEntry.getBackgroundPath();
        String colorHex = currentEntry.getBackgroundColor();
        float opacity = currentEntry.getBackgroundOpacity();
        String scaleTypeStr = currentEntry.getBackgroundScaleType();

        if (path != null && !path.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(path))
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop() // Default scaling if none matches perfectly or as a base
                    .into(backgroundImageView);
            
            backgroundImageView.setVisibility(View.VISIBLE);
            backgroundImageView.setAlpha(1.0f);
            
            // Set scale type AFTER load to ensure it's applied correctly
            if ("FIT_CENTER".equals(scaleTypeStr)) {
                backgroundImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else if ("FIT_XY".equals(scaleTypeStr)) {
                backgroundImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            } else {
                backgroundImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }

            if (backgroundOverlay != null) {
                backgroundOverlay.setVisibility(View.VISIBLE);
                backgroundOverlay.setAlpha(1.0f - opacity);
                backgroundOverlay.setBackgroundColor(android.graphics.Color.WHITE);
            }
        } else if (colorHex != null && !colorHex.isEmpty()) {
            backgroundImageView.setVisibility(View.GONE);
            if (backgroundOverlay != null) {
                backgroundOverlay.setVisibility(View.VISIBLE);
                backgroundOverlay.setAlpha(1.0f);
                try {
                    backgroundOverlay.setBackgroundColor(android.graphics.Color.parseColor(colorHex));
                } catch (Exception e) {
                    backgroundOverlay.setBackgroundColor(android.graphics.Color.WHITE);
                }
            }
        } else {
            backgroundImageView.setVisibility(View.GONE);
            if (backgroundOverlay != null) backgroundOverlay.setVisibility(View.GONE);
            Glide.with(this).clear(backgroundImageView);
        }
    }

    private void showBackgroundSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_background_settings, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        android.widget.SeekBar seekBar = dialogView.findViewById(R.id.seekbar_opacity);
        android.widget.RadioGroup rgScaleType = dialogView.findViewById(R.id.rg_scale_type);
        Button btnSelect = dialogView.findViewById(R.id.btn_select_image);
        Button btnColor = dialogView.findViewById(R.id.btn_select_color);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_bg);

        seekBar.setProgress((int) (currentEntry.getBackgroundOpacity() * 100));

        String currentScale = currentEntry.getBackgroundScaleType();
        if ("FIT_CENTER".equals(currentScale)) rgScaleType.check(R.id.rb_fit_center);
        else if ("FIT_XY".equals(currentScale)) rgScaleType.check(R.id.rb_fit_xy);
        else rgScaleType.check(R.id.rb_center_crop);

        btnSelect.setOnClickListener(v -> {
            backgroundPickerLauncher.launch(new String[]{"image/*"});
            dialog.dismiss();
        });

        btnColor.setOnClickListener(v -> {
            new com.skydoves.colorpickerview.ColorPickerDialog.Builder(requireContext())
                    .setTitle("Pick Color")
                    .setPreferenceName("entry_bg_color")
                    .setPositiveButton("Select", (com.skydoves.colorpickerview.listeners.ColorEnvelopeListener) (envelope, fromUser) -> {
                        currentEntry.setBackgroundPath(null);
                        currentEntry.setBackgroundColor("#" + envelope.getHexCode());
                        applyBackgroundSettings();
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        });

        btnClear.setOnClickListener(v -> {
            currentEntry.setBackgroundPath(null);
            currentEntry.setBackgroundColor(null);
            applyBackgroundSettings();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> {
            float opacity = seekBar.getProgress() / 100f;
            currentEntry.setBackgroundOpacity(opacity);
            
            String selectedScale = "CENTER_CROP";
            int checkedId = rgScaleType.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_fit_center) selectedScale = "FIT_CENTER";
            else if (checkedId == R.id.rb_fit_xy) selectedScale = "FIT_XY";
            
            currentEntry.setBackgroundScaleType(selectedScale);
            applyBackgroundSettings();
        });

        dialog.show();
    }

    private void updatePathsInList(File newDir) {
        for (EntryItem item : itemsList) {
            if (item instanceof Photo) {
                Photo p = (Photo) item;
                File f = new File(p.getPath());
                p.setPath(new File(newDir, f.getName()).getAbsolutePath());
            } else if (item instanceof RouteTrack) {
                RouteTrack t = (RouteTrack) item;
                if (t.getFilePath().startsWith("/")) {
                    File f = new File(t.getFilePath());
                    t.setFilePath(new File(newDir, f.getName()).getAbsolutePath());
                }
            }
        }
        if (currentEntry != null && currentEntry.getBackgroundPath() != null) {
            if (currentEntry.getBackgroundPath().startsWith("/")) {
                File f = new File(currentEntry.getBackgroundPath());
                currentEntry.setBackgroundPath(new File(newDir, f.getName()).getAbsolutePath());
            }
        }
    }

    private void moveOrphanedFilesToDir(File targetDir) {
        File filesDir = requireContext().getFilesDir();
        for (EntryItem item : itemsList) {
            String path = null;
            if (item instanceof Photo) path = ((Photo) item).getPath();
            else if (item instanceof RouteTrack) path = ((RouteTrack) item).getFilePath();

            if (path != null && path.startsWith(filesDir.getAbsolutePath()) && !path.contains("/Entries/")) {
                File sourceFile = new File(path);
                if (sourceFile.exists()) {
                    File targetFile = new File(targetDir, sourceFile.getName());
                    if (sourceFile.renameTo(targetFile)) {
                        if (item instanceof Photo) ((Photo) item).setPath(targetFile.getAbsolutePath());
                        else ((RouteTrack) item).setFilePath(targetFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isEmpty()) return "unnamed_entry";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private File getEntryDirectory(String title) {
        File baseDir = new File(requireContext().getFilesDir(), "Entries");
        if (!baseDir.exists()) baseDir.mkdirs();
        File entryDir = new File(baseDir, sanitizeFilename(title));
        if (!entryDir.exists()) entryDir.mkdirs();
        return entryDir;
    }

    private void moveFilesToTargetDir(File targetDir) {
        File internalFilesDir = requireContext().getFilesDir();
        String targetDirPath = targetDir.getAbsolutePath();

        for (EntryItem item : itemsList) {
            String currentPath = null;
            if (item instanceof Photo) currentPath = ((Photo) item).getPath();
            else if (item instanceof RouteTrack) currentPath = ((RouteTrack) item).getFilePath();

            if (currentPath != null && currentPath.startsWith(internalFilesDir.getAbsolutePath()) 
                && !currentPath.startsWith(targetDirPath)) {
                
                File sourceFile = new File(currentPath);
                if (sourceFile.exists()) {
                    File targetFile = new File(targetDir, sourceFile.getName());
                    if (sourceFile.renameTo(targetFile)) {
                        if (item instanceof Photo) ((Photo) item).setPath(targetFile.getAbsolutePath());
                        else if (item instanceof RouteTrack) ((RouteTrack) item).setFilePath(targetFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void saveEntry() {
        String title = titleInput.getText().toString().trim();
        String tagsText = tagsInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        File baseDir = new File(requireContext().getFilesDir(), "Entries");
        if (!baseDir.exists()) baseDir.mkdirs();
        
        File entryDir = new File(baseDir, sanitizeFilename(title));
        
        // Jeśli edytujemy i zmienił się tytuł -> zmień nazwę folderu
        if (currentEntry != null) {
            String oldSanitized = sanitizeFilename(currentEntry.getTitle());
            File oldDir = new File(baseDir, oldSanitized);
            if (oldDir.exists() && !oldDir.equals(entryDir)) {
                if (oldDir.renameTo(entryDir)) {
                    updatePathsInList(entryDir);
                }
            }
        }
        
        if (!entryDir.exists()) entryDir.mkdirs();
        
        // Przenieś wszystkie luźne pliki do folderu docelowego
        moveFilesToTargetDir(entryDir);

        long entryId;
        if (currentEntry == null) {
            entryId = dbHelper.insertEntry(title);
        } else {
            entryId = currentEntry.getId();
            dbHelper.updateEntryTitle(entryId, title);
            dbHelper.deleteAllNotesForEntry(entryId);
            dbHelper.deleteAllPhotosForEntry(entryId);
            dbHelper.deleteAllPinsForEntry(entryId);
            dbHelper.deleteAllRouteTracksForEntry(entryId);
            dbHelper.deleteAllTagsForEntry(entryId);
        }
        
        // Dalsza część zapisu...

        // Zapisz tagi
        if (!tagsText.isEmpty()) {
            String[] tags = tagsText.split(",");
            for (String tag : tags) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    dbHelper.insertTag(entryId, trimmedTag);
                }
            }
        }

        for (int i = 0; i < itemsList.size(); i++) {
            EntryItem item = itemsList.get(i);
            if (item.isNote()) {
                dbHelper.insertNote(entryId, ((Note) item).getText(), i);
            } else if (item.isMapPin()) {
                MapPin pin = (MapPin) item;
                dbHelper.insertMapPin(entryId, pin.getLabel(), pin.getLatitude(), pin.getLongitude(), i);
            } else if (item instanceof RouteTrack) {
                RouteTrack track = (RouteTrack) item;
                dbHelper.insertRouteTrack(entryId, track.getName(), track.getFilePath(), i);
            } else {
                dbHelper.insertPhoto(entryId, ((Photo) item).getPath(), i);
            }
        }

        if (currentEntry != null) {
            dbHelper.updateEntryBackground(entryId, currentEntry.getBackgroundPath(), 
                currentEntry.getBackgroundColor(), currentEntry.getBackgroundOpacity(), currentEntry.getBackgroundScaleType());
        }

        if (onEntrySavedListener != null) {
            onEntrySavedListener.onEntrySaved();
        }
        dismiss();
    }

    private class ItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_NOTE = 0;
        private static final int TYPE_PHOTO = 1;
        private static final int TYPE_PIN = 2;
        private static final int TYPE_TRACK = 3;

        @Override
        public int getItemViewType(int position) {
            EntryItem item = itemsList.get(position);
            if (item.isNote()) return TYPE_NOTE;
            if (item.isMapPin()) return TYPE_PIN;
            if (item instanceof RouteTrack) return TYPE_TRACK;
            return TYPE_PHOTO;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_NOTE) {
                return new NoteViewHolder(inflater.inflate(R.layout.item_note, parent, false));
            } else if (viewType == TYPE_PIN) {
                return new PinViewHolder(inflater.inflate(R.layout.item_map_pin, parent, false));
            } else if (viewType == TYPE_TRACK) {
                return new TrackViewHolder(inflater.inflate(R.layout.item_route_track, parent, false));
            } else {
                return new PhotoViewHolder(inflater.inflate(R.layout.item_photo, parent, false));
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            EntryItem item = itemsList.get(position);
            View dragHandle = holder.itemView.findViewById(R.id.iv_drag_handle);
            dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder);
                }
                return false;
            });

            // Pobierz kolor motywu (taki jak FAB) z klasy nadrzędnej
            int themeColor = getThemeColor();
            android.content.res.ColorStateList themeTint = android.content.res.ColorStateList.valueOf(themeColor);

            if (holder instanceof NoteViewHolder) {
                Note note = (Note) item;
                NoteViewHolder h = (NoteViewHolder) holder;
                h.noteInput.setText(note.getText());
                h.noteInput.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        note.setText(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                h.btnDelete.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    itemsList.remove(pos);
                    notifyItemRemoved(pos);
                });
            } else if (holder instanceof PinViewHolder) {
                MapPin pin = (MapPin) item;
                PinViewHolder h = (PinViewHolder) holder;
                
                // Zastosuj kolor motywu do ikonek lokalizacji
                if (h.pinIcon != null) h.pinIcon.setImageTintList(themeTint);
                if (h.btnOpenMap instanceof ImageButton) ((ImageButton) h.btnOpenMap).setImageTintList(themeTint);

                h.labelInput.setText(pin.getLabel());
                h.coordsText.setText(String.format(Locale.getDefault(), "%.4f, %.4f", pin.getLatitude(), pin.getLongitude()));
                h.labelInput.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        pin.setLabel(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                h.btnOpenMap.setOnClickListener(v -> {
                    String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", 
                        pin.getLatitude(), pin.getLongitude(), pin.getLatitude(), pin.getLongitude(), pin.getLabel());
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                });
                h.btnDelete.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    itemsList.remove(pos);
                    notifyItemRemoved(pos);
                });
            } else if (holder instanceof TrackViewHolder) {
                RouteTrack track = (RouteTrack) item;
                TrackViewHolder h = (TrackViewHolder) holder;

                // Zastosuj kolor motywu do ikonek śladu
                if (h.trackIcon != null) h.trackIcon.setImageTintList(themeTint);
                if (h.btnOpen instanceof ImageButton) ((ImageButton) h.btnOpen).setImageTintList(themeTint);

                h.nameInput.setText(track.getName());
                h.pathText.setText(track.getFilePath());
                h.nameInput.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        track.setName(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                h.btnOpen.setOnClickListener(v -> {
                    File file = new File(track.getFilePath());
                    if (!file.exists()) {
                        Toast.makeText(getContext(), "File does not exist", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Uri contentUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), 
                        "com.traveltracker.fileprovider", file);

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(contentUri, "application/gpx+xml");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    try {
                        startActivity(Intent.createChooser(intent, "Open track with..."));
                    } catch (Exception e) {
                        // Fallback to generic type
                        intent.setDataAndType(contentUri, "*/*");
                        try {
                            startActivity(Intent.createChooser(intent, "Open track with..."));
                        } catch (Exception ex) {
                            Toast.makeText(getContext(), "No app found to open this file", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                h.btnDelete.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    itemsList.remove(pos);
                    notifyItemRemoved(pos);
                });
            } else if (holder instanceof PhotoViewHolder) {
                Photo photo = (Photo) item;
                PhotoViewHolder h = (PhotoViewHolder) holder;
                Glide.with(AddEditEntryFragment.this).load(photo.getPath())
                        .placeholder(android.R.drawable.ic_menu_gallery).into(h.imageView);
                h.imageView.setOnClickListener(v -> showFullScreenImage(photo.getPath()));
                h.btnDelete.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    itemsList.remove(pos);
                    notifyItemRemoved(pos);
                });
            }
        }

        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        private int getThemeColor() {
            return AddEditEntryFragment.this.getThemeColor();
        }

        class NoteViewHolder extends RecyclerView.ViewHolder {
            EditText noteInput;
            View btnDelete;
            NoteViewHolder(View v) {
                super(v);
                noteInput = v.findViewById(R.id.note_text);
                btnDelete = v.findViewById(R.id.btn_delete_note);
            }
        }

        class PinViewHolder extends RecyclerView.ViewHolder {
            EditText labelInput;
            TextView coordsText;
            View btnOpenMap, btnDelete;
            ImageView pinIcon;
            PinViewHolder(View v) {
                super(v);
                labelInput = v.findViewById(R.id.pin_label);
                coordsText = v.findViewById(R.id.pin_coords);
                btnOpenMap = v.findViewById(R.id.btn_open_map);
                btnDelete = v.findViewById(R.id.btn_delete_pin);
                pinIcon = v.findViewById(R.id.iv_pin_icon);
            }
        }

        class PhotoViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            View btnDelete;
            PhotoViewHolder(View v) {
                super(v);
                imageView = v.findViewById(R.id.photo_image);
                btnDelete = v.findViewById(R.id.btn_delete_photo);
            }
        }

        class TrackViewHolder extends RecyclerView.ViewHolder {
            EditText nameInput;
            TextView pathText;
            View btnOpen, btnDelete;
            ImageView trackIcon;
            TrackViewHolder(View v) {
                super(v);
                nameInput = v.findViewById(R.id.track_name);
                pathText = v.findViewById(R.id.track_path);
                btnOpen = v.findViewById(R.id.btn_open_track);
                btnDelete = v.findViewById(R.id.btn_delete_track);
                trackIcon = v.findViewById(R.id.iv_track_icon);
            }
        }
    }
}
