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

    private List<EntryItem> itemsList = new ArrayList<>();

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
        dbHelper = new DatabaseHelper(getContext());
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
        Button btnSave = view.findViewById(R.id.btn_save_entry);
        Button btnCancel = view.findViewById(R.id.btn_cancel_entry);

        if (currentEntry != null) {
            titleInput.setText(currentEntry.getTitle());
            if (currentEntry.getTags() != null && !currentEntry.getTags().isEmpty()) {
                tagsInput.setText(String.join(", ", currentEntry.getTags()));
            }
            itemsList.clear();
            if (currentEntry.getNotes() != null) itemsList.addAll(currentEntry.getNotes());
            if (currentEntry.getPhotos() != null) itemsList.addAll(currentEntry.getPhotos());
            if (currentEntry.getMapPins() != null) itemsList.addAll(currentEntry.getMapPins());
            
            Collections.sort(itemsList, Comparator.comparingInt(EntryItem::getOrder));
            itemsAdapter.notifyDataSetChanged();
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

        btnSave.setOnClickListener(v -> saveEntry());
        btnCancel.setOnClickListener(v -> dismiss());

        return view;
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
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                int maxWidth = 1920;
                int maxHeight = 1920;
                float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
                if (ratio < 1.0f) {
                    int width = Math.round(ratio * bitmap.getWidth());
                    int height = Math.round(ratio * bitmap.getHeight());
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                }

                String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
                File file = new File(requireContext().getFilesDir(), fileName);
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                out.flush();
                out.close();

                Photo newPhoto = new Photo();
                newPhoto.setPath(file.getAbsolutePath());
                newPhoto.setOrder(itemsList.size());
                itemsList.add(newPhoto);
                itemsAdapter.notifyItemInserted(itemsList.size() - 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullScreenImage(String path) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);
        ImageView fullScreenImage = dialog.findViewById(R.id.full_screen_image);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_full_screen);
        Glide.with(this).load(path).into(fullScreenImage);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveEntry() {
        String title = titleInput.getText().toString().trim();
        String tagsText = tagsInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        long entryId;
        if (currentEntry == null) {
            entryId = dbHelper.insertEntry(title);
        } else {
            entryId = currentEntry.getId();
            dbHelper.updateEntryTitle(entryId, title);
            dbHelper.deleteAllNotesForEntry(entryId);
            dbHelper.deleteAllPhotosForEntry(entryId);
            dbHelper.deleteAllPinsForEntry(entryId);
            dbHelper.deleteAllTagsForEntry(entryId);
        }

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
            } else {
                dbHelper.insertPhoto(entryId, ((Photo) item).getPath(), i);
            }
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

        @Override
        public int getItemViewType(int position) {
            EntryItem item = itemsList.get(position);
            if (item.isNote()) return TYPE_NOTE;
            if (item.isMapPin()) return TYPE_PIN;
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
            PinViewHolder(View v) {
                super(v);
                labelInput = v.findViewById(R.id.pin_label);
                coordsText = v.findViewById(R.id.pin_coords);
                btnOpenMap = v.findViewById(R.id.btn_open_map);
                btnDelete = v.findViewById(R.id.btn_delete_pin);
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
    }
}
