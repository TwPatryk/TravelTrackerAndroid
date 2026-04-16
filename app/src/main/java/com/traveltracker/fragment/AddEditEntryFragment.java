package com.traveltracker.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.traveltracker.R;
import com.traveltracker.database.DatabaseHelper;
import com.traveltracker.database.EntryItem;
import com.traveltracker.database.MapPin;
import com.traveltracker.database.Note;
import com.traveltracker.database.Photo;
import com.traveltracker.database.TravelEntry;

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

    private EditText titleInput;
    private LinearLayout itemsContainer;

    private List<EntryItem> itemsList = new ArrayList<>();

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
        itemsContainer = view.findViewById(R.id.items_container);

        Button btnAddNote = view.findViewById(R.id.btn_add_note);
        Button btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        Button btnAddPin = view.findViewById(R.id.btn_add_pin);
        Button btnSave = view.findViewById(R.id.btn_save_entry);
        Button btnCancel = view.findViewById(R.id.btn_cancel_entry);

        if (currentEntry != null) {
            titleInput.setText(currentEntry.getTitle());
            itemsList.clear();
            if (currentEntry.getNotes() != null) itemsList.addAll(currentEntry.getNotes());
            if (currentEntry.getPhotos() != null) itemsList.addAll(currentEntry.getPhotos());
            if (currentEntry.getMapPins() != null) itemsList.addAll(currentEntry.getMapPins());
            
            Collections.sort(itemsList, Comparator.comparingInt(EntryItem::getOrder));
            renderItems();
        }

        btnAddNote.setOnClickListener(v -> {
            Note newNote = new Note();
            newNote.setText("");
            newNote.setOrder(itemsList.size());
            itemsList.add(newNote);
            renderItems();
        });

        btnAddPhoto.setOnClickListener(v -> {
            Photo newPhoto = new Photo();
            newPhoto.setPath("fake_path_" + System.currentTimeMillis() + ".jpg");
            newPhoto.setOrder(itemsList.size());
            itemsList.add(newPhoto);
            renderItems();
        });

        btnAddPin.setOnClickListener(v -> {
            MapPin newPin = new MapPin();
            newPin.setLabel("");
            // Default coords for testing
            newPin.setLatitude(52.2297); 
            newPin.setLongitude(21.0122);
            newPin.setOrder(itemsList.size());
            itemsList.add(newPin);
            renderItems();
        });

        btnSave.setOnClickListener(v -> saveEntry());
        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }

    private void renderItems() {
        itemsContainer.removeAllViews();
        for (int i = 0; i < itemsList.size(); i++) {
            EntryItem item = itemsList.get(i);
            final int index = i;
            
            View itemView;
            if (item.isNote()) {
                itemView = getLayoutInflater().inflate(R.layout.item_note, itemsContainer, false);
                EditText noteInput = itemView.findViewById(R.id.note_text);
                Note note = (Note) item;
                noteInput.setText(note.getText());
                noteInput.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        note.setText(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                View btnDelete = itemView.findViewById(R.id.btn_delete_note);
                btnDelete.setOnClickListener(v -> {
                    itemsList.remove(index);
                    renderItems();
                });
            } else if (item.isMapPin()) {
                itemView = getLayoutInflater().inflate(R.layout.item_map_pin, itemsContainer, false);
                EditText labelInput = itemView.findViewById(R.id.pin_label);
                TextView coordsText = itemView.findViewById(R.id.pin_coords);
                MapPin pin = (MapPin) item;
                
                labelInput.setText(pin.getLabel());
                coordsText.setText(String.format(Locale.getDefault(), "%.4f, %.4f", pin.getLatitude(), pin.getLongitude()));
                
                labelInput.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        pin.setLabel(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });

                View btnOpenMap = itemView.findViewById(R.id.btn_open_map);
                btnOpenMap.setOnClickListener(v -> {
                    String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", 
                        pin.getLatitude(), pin.getLongitude(), pin.getLatitude(), pin.getLongitude(), pin.getLabel());
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                });

                View btnDelete = itemView.findViewById(R.id.btn_delete_pin);
                btnDelete.setOnClickListener(v -> {
                    itemsList.remove(index);
                    renderItems();
                });
            } else {
                itemView = getLayoutInflater().inflate(R.layout.item_photo, itemsContainer, false);
                ImageView imageView = itemView.findViewById(R.id.photo_image);
                // Placeholder logic for photo
                View btnDelete = itemView.findViewById(R.id.btn_delete_photo);
                btnDelete.setOnClickListener(v -> {
                    itemsList.remove(index);
                    renderItems();
                });
            }
            itemsContainer.addView(itemView);
        }
    }

    private void saveEntry() {
        String title = titleInput.getText().toString().trim();
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
}
