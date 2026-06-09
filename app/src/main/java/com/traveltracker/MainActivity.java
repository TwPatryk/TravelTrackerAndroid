package com.traveltracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.traveltracker.adapter.EntryAdapter;
import com.traveltracker.database.DatabaseHelper;
import com.traveltracker.database.TravelEntry;
import com.traveltracker.util.UiUtils;
import com.traveltracker.fragment.AddEditEntryFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private EntryAdapter adapter;
    private DatabaseHelper dbHelper;
    private final List<TravelEntry> allEntries = new ArrayList<>();
    private final List<TravelEntry> filteredEntries = new ArrayList<>();
    private BackupManager backupManager;
    private UiSettingsManager uiSettingsManager;

    private Toolbar toolbar;
    private FloatingActionButton fab;

    private EditText searchEditText;
    private LinearLayout filterContainer;
    private ChipGroup tagChipGroup;
    private final List<String> selectedTags = new ArrayList<>();
    private ImageView backgroundImageView;
    private ImageView toolbarBackgroundImageView;
    private View mainBackgroundOverlay;
    private View toolbarBackgroundOverlay;
    private ImageView fabBackgroundImageView;
    private View fabBackgroundOverlay;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> {
                if (uri != null) backupManager.exportDatabase(uri);
            }
    );

    private final ActivityResultLauncher<String> exportAllLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"),
            uri -> {
                if (uri != null) backupManager.exportAllData(uri);
            }
    );

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) backupManager.importDatabase(uri);
            }
    );

    private final ActivityResultLauncher<String[]> backgroundPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    dbHelper.setGlobalSetting("main_bg_path", uri.toString());
                    uiSettingsManager.applyBackgroundSettings();
                }
            }
    );

    private final ActivityResultLauncher<String[]> toolbarBackgroundPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    dbHelper.setGlobalSetting("toolbar_bg_path", uri.toString());
                    uiSettingsManager.applyBackgroundSettings();
                }
            }
    );

    private final ActivityResultLauncher<String[]> fabBackgroundPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    dbHelper.setGlobalSetting("fab_bg_path", uri.toString());
                    uiSettingsManager.applyBackgroundSettings();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Włączenie Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Ustawienie paska na przezroczysty i wyłączenie wymuszonego kontrastu (Android 10+)
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarContrastEnforced(false);
        }

        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);
        backupManager = new BackupManager(this, dbHelper, new BackupManager.OnBackupListener() {
            @Override
            public void onRestoreComplete() {
                loadInitialData();
            }

            @Override
            public void onRunOnUiThread(Runnable runnable) {
                runOnUiThread(runnable);
            }
        });
        
        uiSettingsManager = new UiSettingsManager(this, dbHelper, prefix -> {
            if ("main_bg_".equals(prefix)) backgroundPickerLauncher.launch(new String[]{"image/*"});
            else if ("toolbar_bg_".equals(prefix)) toolbarBackgroundPickerLauncher.launch(new String[]{"image/*"});
            else if ("fab_bg_".equals(prefix)) fabBackgroundPickerLauncher.launch(new String[]{"image/*"});
        });

        initViews();
        setupRecyclerView();
        setupFilters();
        setupDrawer();

        try {
            // Apply edge-to-edge
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                
                View toolbarContainer = findViewById(R.id.main_toolbar).getParent() instanceof View ? (View)findViewById(R.id.main_toolbar).getParent() : null;
                if (toolbarContainer != null) {
                    toolbarContainer.setPadding(0, systemBars.top, 0, 0);
                }
                
                if (recyclerView != null) {
                    recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), 
                                           recyclerView.getPaddingRight(), systemBars.bottom + (int)(8 * getResources().getDisplayMetrics().density));
                    recyclerView.setClipToPadding(false);
                }

                if (fab != null && fab.getParent() instanceof View) {
                    View fabContainer = (View) fab.getParent();
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fabContainer.getLayoutParams();
                    lp.bottomMargin = systemBars.bottom + (int)(16 * getResources().getDisplayMetrics().density);
                    fabContainer.setLayoutParams(lp);
                }
                
                return insets;
            });
            
            new Thread(this::loadInitialData).start();

        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in onCreate", e);
        }
    }

    private void loadInitialData() {
        final List<TravelEntry> entries = dbHelper.getAllEntries();

        runOnUiThread(() -> {
            uiSettingsManager.applyBackgroundSettings();
            allEntries.clear();
            allEntries.addAll(entries);
            applyFilters();
            updateTagChips();
            updateDrawerMenu();
            android.util.Log.d("MainActivity", "Data loaded and UI updated.");
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        recyclerView = findViewById(R.id.entries_recycler_view);
        tagChipGroup = findViewById(R.id.tag_chip_group);
        backgroundImageView = findViewById(R.id.main_background_image);
        toolbarBackgroundImageView = findViewById(R.id.toolbar_background_image);
        mainBackgroundOverlay = findViewById(R.id.main_background_overlay);
        toolbarBackgroundOverlay = findViewById(R.id.toolbar_background_overlay);
        fabBackgroundImageView = findViewById(R.id.fab_background_image);
        fabBackgroundOverlay = findViewById(R.id.fab_background_overlay);
        fab = findViewById(R.id.fab_add_entry);
        toolbar = findViewById(R.id.main_toolbar);

        uiSettingsManager.setViews(backgroundImageView, toolbarBackgroundImageView, mainBackgroundOverlay, 
                                 toolbarBackgroundOverlay, fabBackgroundImageView, fabBackgroundOverlay, 
                                 toolbar, fab, adapter);

        fab.setOnClickListener(v -> openAddEditFragment(null));

        uiSettingsManager.applyThemeSettings();
    }

    private void setupDrawer() {
        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.nav_view)));

        ImageButton filterButton = findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> {
            if (filterContainer.getVisibility() == View.VISIBLE) {
                filterContainer.setVisibility(View.GONE);
                UiUtils.hideKeyboard(this);
            } else {
                filterContainer.setVisibility(View.VISIBLE);
            }
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_all_entries) {
                clearFilters();
            } else if (id == R.id.nav_backup_export) {
                exportLauncher.launch("travel_tracker_backup.db");
            } else if (id == R.id.nav_backup_import) {
                importLauncher.launch(new String[]{"application/octet-stream"});
            } else if (id == R.id.nav_export_all) {
                exportAllLauncher.launch("travel_tracker_full_backup.zip");
            } else if (id == R.id.nav_theme_color) {
                uiSettingsManager.showThemeChooserDialog();
            } else if (id == R.id.nav_main_bg) {
                uiSettingsManager.showBackgroundSettingsDialog("main_bg_");
            } else if (id == R.id.nav_toolbar_bg) {
                uiSettingsManager.showBackgroundSettingsDialog("toolbar_bg_");
            } else if (id == R.id.nav_fab_bg) {
                uiSettingsManager.showBackgroundSettingsDialog("fab_bg_");
            } else if (id == R.id.nav_item_style) {
                uiSettingsManager.showBackgroundSettingsDialog("item_bg_");
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
            }
        });
        recyclerView.setAdapter(adapter);
        
        if (uiSettingsManager != null) {
            uiSettingsManager.setViews(backgroundImageView, toolbarBackgroundImageView, mainBackgroundOverlay, 
                                     toolbarBackgroundOverlay, fabBackgroundImageView, fabBackgroundOverlay, 
                                     toolbar, fab, adapter);
        }
    }

    private void updateTagChips() {
        tagChipGroup.removeAllViews();
        List<String> tags = dbHelper.getAllUniqueTags();
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(selectedTags.contains(tag));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedTags.add(tag);
                } else {
                    selectedTags.remove(tag);
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
            menu.add(R.id.group_entries, (int)entry.getId(), 0, entry.getTitle())
                .setIcon(R.drawable.ic_place);
        }
    }

    private void setupFilters() {
        filterContainer = findViewById(R.id.filter_container);
        searchEditText = findViewById(R.id.search_input);
        
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void applyFilters() {
        String searchQuery = searchEditText.getText().toString().toLowerCase().trim();
        boolean toVisitSelected = selectedTags.contains("do zwiedzenia");

        filteredEntries.clear();

        for (TravelEntry entry : allEntries) {
            boolean hasToVisitTag = entry.getTags() != null && entry.getTags().contains("do zwiedzenia");

            if (hasToVisitTag && !toVisitSelected) {
                continue;
            }

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

            if (!matchesSearch && entry.getTags() != null) {
                for (String tag : entry.getTags()) {
                    if (tag.toLowerCase().contains(searchQuery)) {
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
        updateTagChips();
        applyFilters();
    }

    private void openAddEditFragment(TravelEntry entry) {
        AddEditEntryFragment fragment = AddEditEntryFragment.newInstance(entry);
        fragment.setOnEntrySavedListener(this::loadInitialData);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content, fragment)
                .addToBackStack(null)
                .commit();
    }
}
