package com.traveltracker;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.traveltracker.adapter.EntryAdapter;
import com.traveltracker.database.DatabaseHelper;
import com.traveltracker.database.TravelEntry;
import com.traveltracker.fragment.AddEditEntryFragment;

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
        FloatingActionButton fab = findViewById(R.id.fab_add_entry);

        fab.setOnClickListener(v -> openAddEditFragment(null));
    }

    private void setupDrawer() {
        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.nav_view)));

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
        recyclerView.setAdapter(adapter);
    }

    private void loadEntries() {
        allEntries = dbHelper.getAllEntries();
        applyFilters();
        updateDrawerMenu();
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
            } else {
                filterContainer.setVisibility(android.view.View.GONE);
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
            boolean matchesSearch = searchQuery.isEmpty() ||
                    entry.getTitle().toLowerCase().contains(searchQuery);

            if (matchesSearch) {
                filteredEntries.add(entry);
            }
        }

        adapter.updateEntries(filteredEntries);
    }

    private void clearFilters() {
        searchEditText.setText("");
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
}