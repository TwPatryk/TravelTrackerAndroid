package com.traveltracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.traveltracker.adapter.EntryAdapter;
import com.traveltracker.database.DatabaseHelper;
import com.traveltracker.database.TravelEntry;
import com.traveltracker.fragment.AddEditEntryFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private EntryAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<TravelEntry> allEntries;
    private List<TravelEntry> filteredEntries;

    // Filtry
    private EditText searchEditText;
    private LinearLayout filterContainer;
    private ChipGroup tagChipGroup;
    private List<String> selectedTags = new ArrayList<>();

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri != null) {
                    exportDatabase(uri);
                }
            });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importDatabase(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        allEntries = new ArrayList<>();
        filteredEntries = new ArrayList<>();

        initViews();
        setupDrawer();
        setupRecyclerView();
        setupFilters();
        loadEntries();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        recyclerView = findViewById(R.id.entries_recycler_view);
        tagChipGroup = findViewById(R.id.tag_chip_group);
        FloatingActionButton fab = findViewById(R.id.fab_add_entry);

        fab.setOnClickListener(v -> openAddEditFragment(null));

        Button btnExport = findViewById(R.id.btn_export);
        Button btnImport = findViewById(R.id.btn_import);

        btnExport.setOnClickListener(v -> exportLauncher.launch("travel_tracker_backup.db"));
        btnImport.setOnClickListener(v -> importLauncher.launch(new String[]{"application/octet-stream", "*/*"}));
    }

    private void setupDrawer() {
        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.drawer_content)));

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_all_entries) {
                clearFilters();
                loadEntries();
            } else if (item.getGroupId() == R.id.group_entries) {
                for (TravelEntry entry : allEntries) {
                    if (entry.getId() == id) {
                        openAddEditFragment(entry);
                        break;
                    }
                }
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntryAdapter(filteredEntries, new EntryAdapter.OnEntryClickListener() {
            @Override
            public void onEntryClick(TravelEntry entry) {
                openAddEditFragment(entry);
            }

            @Override
            public void onEntryLongClick(TravelEntry entry) {
                // Opcja usunięcia
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Entry")
                        .setMessage("Are you sure you want to delete \"" + entry.getTitle() + "\"?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dbHelper.deleteEntry(entry.getId());
                            loadEntries();
                            Toast.makeText(MainActivity.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                java.util.Collections.swap(filteredEntries, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Zapisz nową kolejność w bazie danych
                for (int i = 0; i < filteredEntries.size(); i++) {
                    dbHelper.updateEntryOrder(filteredEntries.get(i).getId(), i);
                }
                // Odśwież listę główną (allEntries), aby zachować spójność
                allEntries = dbHelper.getAllEntries();
                updateDrawerMenu();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setItemTouchHelper(itemTouchHelper);

        recyclerView.setAdapter(adapter);
    }

    private void loadEntries() {
        allEntries = dbHelper.getAllEntries();
        updateTagChips();
        applyFilters();
        updateDrawerMenu();
    }

    private void updateTagChips() {
        List<String> allTags = dbHelper.getAllUniqueTags();
        tagChipGroup.removeAllViews();
        
        for (String tag : allTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(selectedTags.contains(tag));
            
            // Używamy hashCode nazwy tagu do wygenerowania stałego odcienia (Hue)
            // Wynik hashCode może być ujemny, więc bierzemy wartość bezwzględną
            float hue = (Math.abs(tag.hashCode()) % 360);
            
            // Saturation 0.8f (żywe) i Value 0.9f (jasne) dla energetycznego efektu
            int color = android.graphics.Color.HSVToColor(new float[]{hue, 0.8f, 0.9f});
            
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
            chip.setTextColor(android.graphics.Color.WHITE);
            
            if (chip.isChecked()) {
                chip.setChipStrokeWidth(5f);
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK));
            } else {
                chip.setChipStrokeWidth(0f);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedTags.contains(tag)) selectedTags.add(tag);
                    chip.setChipStrokeWidth(5f);
                    chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK));
                } else {
                    selectedTags.remove(tag);
                    chip.setChipStrokeWidth(0f);
                }
                applyFilters();
            });
            
            tagChipGroup.addView(chip);
        }
    }

    private void updateDrawerMenu() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        android.view.Menu menu = navigationView.getMenu();
        menu.removeGroup(R.id.group_entries);

        for (TravelEntry entry : allEntries) {
            menu.add(R.id.group_entries, (int) entry.getId(), android.view.Menu.NONE, entry.getTitle())
                    .setIcon(R.drawable.ic_list);
        }
    }

    private void setupFilters() {
        searchEditText = findViewById(R.id.search_input);
        filterContainer = findViewById(R.id.filter_container);

        // Przycisk do pokazywania/ukrywania filtrów
        ImageButton filterButton = findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> {
            if (filterContainer.getVisibility() == android.view.View.GONE) {
                filterContainer.setVisibility(android.view.View.VISIBLE);
                searchEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                filterContainer.setVisibility(android.view.View.GONE);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                }
            }
        });

        // Filtrowanie po wpisaniu tekstu
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void applyFilters() {
        String searchQuery = searchEditText.getText().toString().toLowerCase().trim();

        filteredEntries.clear();

        for (TravelEntry entry : allEntries) {
            // Sprawdź czy wpis posiada WSZYSTKIE wybrane tagi (AND)
            boolean matchesTags = true;
            if (!selectedTags.isEmpty()) {
                if (entry.getTags() == null) {
                    matchesTags = false;
                } else {
                    for (String selected : selectedTags) {
                        if (!entry.getTags().contains(selected)) {
                            matchesTags = false;
                            break;
                        }
                    }
                }
            }

            if (!matchesTags) continue;

            boolean matchesSearch = searchQuery.isEmpty() ||
                    entry.getTitle().toLowerCase().contains(searchQuery);

            // Sprawdź czy fraza występuje w tagach
            if (!matchesSearch && !searchQuery.isEmpty() && entry.getTags() != null) {
                for (String tag : entry.getTags()) {
                    if (tag.toLowerCase().contains(searchQuery)) {
                        matchesSearch = true;
                        break;
                    }
                }
            }

            // Sprawdź czy fraza występuje w notatkach
            if (!matchesSearch && !searchQuery.isEmpty() && entry.getNotes() != null) {
                for (com.traveltracker.database.Note note : entry.getNotes()) {
                    if (note.getText() != null && note.getText().toLowerCase().contains(searchQuery)) {
                        matchesSearch = true;
                        break;
                    }
                }
            }

            // Sprawdź czy fraza występuje w etykietach pinezek
            if (!matchesSearch && !searchQuery.isEmpty() && entry.getMapPins() != null) {
                for (com.traveltracker.database.MapPin pin : entry.getMapPins()) {
                    if (pin.getLabel() != null && pin.getLabel().toLowerCase().contains(searchQuery)) {
                        matchesSearch = true;
                        break;
                    }
                }
            }

            if (matchesSearch) {
                filteredEntries.add(entry);
            }
        }

        adapter.updateEntries(filteredEntries);
    }

    private void clearFilters() {
        searchEditText.setText("");
        selectedTags.clear();
        applyFilters();
    }

    private void openAddEditFragment(TravelEntry entry) {
        AddEditEntryFragment fragment = AddEditEntryFragment.newInstance(entry);
        fragment.setOnEntrySavedListener(() -> {
            loadEntries();
            drawerLayout.closeDrawers();
        });
        fragment.show(getSupportFragmentManager(), "AddEditEntryFragment");
    }

    private void exportDatabase(Uri uri) {
        File dbFile = getDatabasePath("travel_tracker.db");
        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = getContentResolver().openOutputStream(uri)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            Toast.makeText(this, "Database exported successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(Uri uri) {
        File dbFile = getDatabasePath("travel_tracker.db");
        
        // Close DB before import
        dbHelper.close();

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dbFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            Toast.makeText(this, "Database imported successfully. Restarting...", Toast.LENGTH_SHORT).show();
            
            // Reload data
            dbHelper = new DatabaseHelper(this);
            loadEntries();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}