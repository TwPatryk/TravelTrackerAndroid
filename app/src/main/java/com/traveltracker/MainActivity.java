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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

    private androidx.appcompat.widget.Toolbar toolbar;
    private FloatingActionButton fab;

    // Filtry
    private EditText searchEditText;
    private LinearLayout filterContainer;
    private ChipGroup tagChipGroup;
    private List<String> selectedTags = new ArrayList<>();
    private android.widget.ImageView backgroundImageView;
    private android.widget.ImageView toolbarBackgroundImageView;
    private android.view.View mainBackgroundOverlay;
    private android.view.View toolbarBackgroundOverlay;
    private android.widget.ImageView fabBackgroundImageView;
    private android.view.View fabBackgroundOverlay;

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

    private final ActivityResultLauncher<String[]> backgroundPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    dbHelper.setGlobalSetting("main_bg_path", uri.toString());
                    dbHelper.setGlobalSetting("main_bg_color", null);
                    applyBackgroundSettings();
                }
            });

    private final ActivityResultLauncher<String[]> toolbarBackgroundPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    dbHelper.setGlobalSetting("toolbar_bg_path", uri.toString());
                    dbHelper.setGlobalSetting("toolbar_bg_color", null);
                    applyBackgroundSettings();
                }
            });

    private final ActivityResultLauncher<String[]> fabBackgroundPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    dbHelper.setGlobalSetting("fab_bg_path", uri.toString());
                    dbHelper.setGlobalSetting("fab_bg_color", null);
                    applyBackgroundSettings();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("MainActivity", "=== APP STARTING ===");
        super.onCreate(savedInstanceState);
        try {
            android.util.Log.d("MainActivity", "Setting content view...");
            setContentView(R.layout.activity_main);
            android.util.Log.d("MainActivity", "Content view set.");

            dbHelper = DatabaseHelper.getInstance(this);
            allEntries = new ArrayList<>();
            filteredEntries = new ArrayList<>();

            initViews();
            setupDrawer();
            setupRecyclerView();
            setupFilters();
            applyBackgroundSettings();
            loadEntries();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in onCreate", e);
            Toast.makeText(this, "Critical error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void applyBackgroundSettings() {
        try {
            // Main Background
            String path = dbHelper.getGlobalSetting("main_bg_path");
            String colorHex = dbHelper.getGlobalSetting("main_bg_color");
            String opacityStr = dbHelper.getGlobalSetting("main_bg_opacity");
            String scaleTypeStr = dbHelper.getGlobalSetting("main_bg_scale_type");

            float opacity = 1.0f;
            try {
                if (opacityStr != null) opacity = Float.parseFloat(opacityStr);
            } catch (NumberFormatException ignored) {}

            android.widget.ImageView.ScaleType scaleType = android.widget.ImageView.ScaleType.CENTER_CROP;
            try {
                if (scaleTypeStr != null) scaleType = android.widget.ImageView.ScaleType.valueOf(scaleTypeStr);
            } catch (IllegalArgumentException ignored) {}

            if (path != null && !path.isEmpty()) {
                Glide.with(this)
                        .load(Uri.parse(path))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(backgroundImageView);
                
                backgroundImageView.setVisibility(android.view.View.VISIBLE);
                backgroundImageView.setScaleType(scaleType);
                backgroundImageView.setAlpha(1.0f); 
                
                if (mainBackgroundOverlay != null) {
                    mainBackgroundOverlay.setVisibility(android.view.View.VISIBLE);
                    mainBackgroundOverlay.setAlpha(1.0f - opacity);
                    mainBackgroundOverlay.setBackgroundColor(android.graphics.Color.WHITE);
                }
            } else if (colorHex != null && !colorHex.isEmpty()) {
                backgroundImageView.setVisibility(android.view.View.GONE);
                if (mainBackgroundOverlay != null) {
                    mainBackgroundOverlay.setVisibility(android.view.View.VISIBLE);
                    mainBackgroundOverlay.setAlpha(1.0f);
                    mainBackgroundOverlay.setBackgroundColor(android.graphics.Color.parseColor(colorHex));
                }
            } else {
                backgroundImageView.setVisibility(android.view.View.GONE);
                if (mainBackgroundOverlay != null) mainBackgroundOverlay.setVisibility(android.view.View.GONE);
                Glide.with(this).clear(backgroundImageView);
            }

            // Toolbar Background
            String tPath = dbHelper.getGlobalSetting("toolbar_bg_path");
            String tColorHex = dbHelper.getGlobalSetting("toolbar_bg_color");
            String tOpacityStr = dbHelper.getGlobalSetting("toolbar_bg_opacity");
            String tScaleTypeStr = dbHelper.getGlobalSetting("toolbar_bg_scale_type");

            float tOpacity = 1.0f;
            try {
                if (tOpacityStr != null) tOpacity = Float.parseFloat(tOpacityStr);
            } catch (NumberFormatException ignored) {}

            android.widget.ImageView.ScaleType tScaleType = android.widget.ImageView.ScaleType.CENTER_CROP;
            try {
                if (tScaleTypeStr != null) tScaleType = android.widget.ImageView.ScaleType.valueOf(tScaleTypeStr);
            } catch (IllegalArgumentException ignored) {}

            if (tPath != null && !tPath.isEmpty()) {
                Glide.with(this)
                        .load(Uri.parse(tPath))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(toolbarBackgroundImageView);
                toolbarBackgroundImageView.setVisibility(android.view.View.VISIBLE);
                toolbarBackgroundImageView.setScaleType(tScaleType);
                
                if (toolbarBackgroundOverlay != null) {
                    toolbarBackgroundOverlay.setVisibility(android.view.View.VISIBLE);
                    toolbarBackgroundOverlay.setAlpha(1.0f - tOpacity);
                    toolbarBackgroundOverlay.setBackgroundColor(android.graphics.Color.WHITE);
                }
                if (toolbar != null) toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            } else if (tColorHex != null && !tColorHex.isEmpty()) {
                toolbarBackgroundImageView.setVisibility(android.view.View.GONE);
                if (toolbarBackgroundOverlay != null) {
                    toolbarBackgroundOverlay.setVisibility(android.view.View.VISIBLE);
                    toolbarBackgroundOverlay.setAlpha(1.0f);
                    toolbarBackgroundOverlay.setBackgroundColor(android.graphics.Color.parseColor(tColorHex));
                }
                if (toolbar != null) toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            } else {
                toolbarBackgroundImageView.setVisibility(android.view.View.GONE);
                if (toolbarBackgroundOverlay != null) toolbarBackgroundOverlay.setVisibility(android.view.View.GONE);
                Glide.with(this).clear(toolbarBackgroundImageView);
                applyThemeSettings(); 
            }

            // FAB Background (independent)
            String fPath = dbHelper.getGlobalSetting("fab_bg_path");
            String fColorHex = dbHelper.getGlobalSetting("fab_bg_color");
            String fOpacityStr = dbHelper.getGlobalSetting("fab_bg_opacity");
            String fScaleTypeStr = dbHelper.getGlobalSetting("fab_bg_scale_type");

            float fOpacity = 1.0f;
            try {
                if (fOpacityStr != null) fOpacity = Float.parseFloat(fOpacityStr);
            } catch (NumberFormatException ignored) {}

            android.widget.ImageView.ScaleType fScaleType = android.widget.ImageView.ScaleType.CENTER_CROP;
            try {
                if (fScaleTypeStr != null) fScaleType = android.widget.ImageView.ScaleType.valueOf(fScaleTypeStr);
            } catch (IllegalArgumentException ignored) {}

            int finalFabColor = 0;

            if (fPath != null && !fPath.isEmpty()) {
                if (fabBackgroundImageView != null) {
                    Glide.with(this)
                            .load(Uri.parse(fPath))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(fabBackgroundImageView);
                    fabBackgroundImageView.setVisibility(android.view.View.VISIBLE);
                    fabBackgroundImageView.setScaleType(fScaleType);
                }
                if (fabBackgroundOverlay != null) {
                    fabBackgroundOverlay.setVisibility(android.view.View.VISIBLE);
                    fabBackgroundOverlay.setAlpha(1.0f - fOpacity);
                    fabBackgroundOverlay.setBackgroundColor(android.graphics.Color.WHITE);
                }
                if (fab != null) {
                    fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                }
                // When using image, we don't change status bar based on it easily, 
                // but let's default to theme color for status bar if no specific color is set
            } else if (fColorHex != null && !fColorHex.isEmpty()) {
                if (fabBackgroundImageView != null) fabBackgroundImageView.setVisibility(android.view.View.GONE);
                finalFabColor = android.graphics.Color.parseColor(fColorHex);
                if (fabBackgroundOverlay != null) {
                    fabBackgroundOverlay.setVisibility(android.view.View.VISIBLE);
                    fabBackgroundOverlay.setAlpha(1.0f);
                    fabBackgroundOverlay.setBackgroundColor(finalFabColor);
                }
                if (fab != null) {
                    fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                }
            } else {
                if (fabBackgroundImageView != null) {
                    fabBackgroundImageView.setVisibility(android.view.View.GONE);
                    Glide.with(this).clear(fabBackgroundImageView);
                }
                if (fabBackgroundOverlay != null) fabBackgroundOverlay.setVisibility(android.view.View.GONE);
                
                String themeColorHex = dbHelper.getGlobalSetting("theme_color");
                finalFabColor = (themeColorHex != null) ? android.graphics.Color.parseColor(themeColorHex) : getResources().getColor(R.color.primary);
                if (fab != null) {
                    fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(finalFabColor));
                }
            }

            // Status Bar Sync Logic
            int statusBarColor;
            if (fColorHex != null && !fColorHex.isEmpty()) {
                statusBarColor = android.graphics.Color.parseColor(fColorHex);
            } else {
                String themeColorHex = dbHelper.getGlobalSetting("theme_color");
                statusBarColor = (themeColorHex != null) ? android.graphics.Color.parseColor(themeColorHex) : getResources().getColor(R.color.primary);
            }
            getWindow().setStatusBarColor(statusBarColor);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showBackgroundSettingsDialog(final String prefix) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_background_settings, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        android.widget.TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        if ("toolbar_bg_".equals(prefix)) tvTitle.setText("Toolbar Background");
        else if ("fab_bg_".equals(prefix)) tvTitle.setText("FAB Background");
        else tvTitle.setText("Main Background");

        android.widget.SeekBar seekBar = dialogView.findViewById(R.id.seekbar_opacity);
        android.widget.RadioGroup rgScaleType = dialogView.findViewById(R.id.rg_scale_type);
        Button btnSelect = dialogView.findViewById(R.id.btn_select_image);
        Button btnSelectColor = dialogView.findViewById(R.id.btn_select_color);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_bg);

        String currentOpacity = dbHelper.getGlobalSetting(prefix + "opacity");
        seekBar.setProgress((int) ((currentOpacity != null ? Float.parseFloat(currentOpacity) : 1.0f) * 100));

        String currentScale = dbHelper.getGlobalSetting(prefix + "scale_type");
        if ("FIT_CENTER".equals(currentScale)) rgScaleType.check(R.id.rb_fit_center);
        else if ("FIT_XY".equals(currentScale)) rgScaleType.check(R.id.rb_fit_xy);
        else rgScaleType.check(R.id.rb_center_crop);

        btnSelect.setOnClickListener(v -> {
            if ("toolbar_bg_".equals(prefix)) {
                toolbarBackgroundPickerLauncher.launch(new String[]{"image/*"});
            } else if ("fab_bg_".equals(prefix)) {
                fabBackgroundPickerLauncher.launch(new String[]{"image/*"});
            } else {
                backgroundPickerLauncher.launch(new String[]{"image/*"});
            }
            dialog.dismiss();
        });

        btnSelectColor.setOnClickListener(v -> {
            showColorChooserForPrefix(prefix);
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            dbHelper.setGlobalSetting(prefix + "path", null);
            dbHelper.setGlobalSetting(prefix + "color", null);
            applyBackgroundSettings();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> {
            float opacity = seekBar.getProgress() / 100f;
            dbHelper.setGlobalSetting(prefix + "opacity", String.valueOf(opacity));
            
            String selectedScale = "CENTER_CROP";
            int checkedId = rgScaleType.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_fit_center) selectedScale = "FIT_CENTER";
            else if (checkedId == R.id.rb_fit_xy) selectedScale = "FIT_XY";
            
            dbHelper.setGlobalSetting(prefix + "scale_type", selectedScale);
            applyBackgroundSettings();
        });

        dialog.show();
    }

    private void showColorChooserForPrefix(String prefix) {
        String[] colorNames = {
                "Orange (Default)", "Amber Glow", "Sunflower Yellow", "Vibrant Lime",
                "Light Green", "Forest Green", "Mint Teal", "Cyan Dream",
                "Sky Blue", "Electric Blue", "Indigo Night", "Deep Purple",
                "Vivid Violet", "Hot Pink", "Energetic Red", "Deep Orange", "Soft Coral",
                "Custom..."
        };

        int[] colors = {
                0xFFFF3D00, 0xFFFFC107, 0xFFFFEB3B, 0xFFCDDC39,
                0xFF8BC34A, 0xFF4CAF50, 0xFF009688, 0xFF00BCD4,
                0xFF03A9F4, 0xFF2196F3, 0xFF3F51B5, 0xFF673AB7,
                0xFF9C27B0, 0xFFE91E63, 0xFFF44336, 0xFFFF5722, 0xFFFF7F50
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Choose Color")
                .setItems(colorNames, (dialog, which) -> {
                    if (which == colorNames.length - 1) {
                        showCustomColorPickerDialogForPrefix(prefix);
                    } else {
                        int selectedColor = colors[which];
                        dbHelper.setGlobalSetting(prefix + "color", String.format("#%08X", selectedColor));
                        dbHelper.setGlobalSetting(prefix + "path", null); // Clear path if color chosen
                        applyBackgroundSettings();
                    }
                })
                .show();
    }

    private void showCustomColorPickerDialogForPrefix(String prefix) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final android.view.View colorPreview = new android.view.View(this);
        String currentColorHex = dbHelper.getGlobalSetting(prefix + "color");
        int currentColor = (currentColorHex != null) ? android.graphics.Color.parseColor(currentColorHex) : 0xFFFF3D00;
        
        android.widget.LinearLayout.LayoutParams previewParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) (60 * getResources().getDisplayMetrics().density));
        previewParams.setMargins(0, 0, 0, padding);
        colorPreview.setLayoutParams(previewParams);
        colorPreview.setBackgroundColor(currentColor);
        layout.addView(colorPreview);

        final android.widget.SeekBar seekR = createColorSeekBar(android.graphics.Color.red(currentColor));
        final android.widget.SeekBar seekG = createColorSeekBar(android.graphics.Color.green(currentColor));
        final android.widget.SeekBar seekB = createColorSeekBar(android.graphics.Color.blue(currentColor));

        layout.addView(createLabel("Red"));
        layout.addView(seekR);
        layout.addView(createLabel("Green"));
        layout.addView(seekG);
        layout.addView(createLabel("Blue"));
        layout.addView(seekB);

        android.widget.SeekBar.OnSeekBarChangeListener listener = new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int color = android.graphics.Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                colorPreview.setBackgroundColor(color);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        };

        seekR.setOnSeekBarChangeListener(listener);
        seekG.setOnSeekBarChangeListener(listener);
        seekB.setOnSeekBarChangeListener(listener);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Compose Custom Color")
                .setView(layout)
                .setPositiveButton("Apply", (dialog, which) -> {
                    int color = android.graphics.Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                    dbHelper.setGlobalSetting(prefix + "color", String.format("#%08X", color));
                    dbHelper.setGlobalSetting(prefix + "path", null); // Clear path if color chosen
                    applyBackgroundSettings();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        fab.setOnClickListener(v -> openAddEditFragment(null));

        Button btnExport = findViewById(R.id.btn_export);
        Button btnImport = findViewById(R.id.btn_import);
        Button btnSetBg = findViewById(R.id.btn_set_background);
        Button btnSetToolbarBg = findViewById(R.id.btn_set_toolbar_background);
        Button btnSetFabBg = findViewById(R.id.btn_set_fab_background);
        Button btnChangeTheme = findViewById(R.id.btn_change_theme);

        btnExport.setOnClickListener(v -> exportLauncher.launch("travel_tracker_backup.db"));
        btnImport.setOnClickListener(v -> importLauncher.launch(new String[]{"application/octet-stream", "*/*"}));
        btnSetBg.setOnClickListener(v -> showBackgroundSettingsDialog("main_bg_"));
        btnSetToolbarBg.setOnClickListener(v -> showBackgroundSettingsDialog("toolbar_bg_"));
        btnSetFabBg.setOnClickListener(v -> showBackgroundSettingsDialog("fab_bg_"));
        btnChangeTheme.setOnClickListener(v -> showThemeChooserDialog());

        applyThemeSettings();
    }

    private void showThemeChooserDialog() {
        String[] colorNames = {
                "Orange (Default)", "Amber Glow", "Sunflower Yellow", "Vibrant Lime",
                "Light Green", "Forest Green", "Mint Teal", "Cyan Dream",
                "Sky Blue", "Electric Blue", "Indigo Night", "Deep Purple",
                "Vivid Violet", "Hot Pink", "Energetic Red", "Deep Orange", "Soft Coral",
                "Custom..."
        };

        int[] colors = {
                0xFFFF3D00, 0xFFFFC107, 0xFFFFEB3B, 0xFFCDDC39,
                0xFF8BC34A, 0xFF4CAF50, 0xFF009688, 0xFF00BCD4,
                0xFF03A9F4, 0xFF2196F3, 0xFF3F51B5, 0xFF673AB7,
                0xFF9C27B0, 0xFFE91E63, 0xFFF44336, 0xFFFF5722, 0xFFFF7F50
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Choose Theme Color")
                .setItems(colorNames, (dialog, which) -> {
                    if (which == colorNames.length - 1) {
                        showCustomColorPickerDialog();
                    } else {
                        int selectedColor = colors[which];
                        dbHelper.setGlobalSetting("theme_color", String.format("#%08X", selectedColor));
                        applyThemeSettings();
                    }
                })
                .show();
    }

    private void showCustomColorPickerDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final android.view.View colorPreview = new android.view.View(this);
        String currentColorHex = dbHelper.getGlobalSetting("theme_color");
        int currentColor = (currentColorHex != null) ? android.graphics.Color.parseColor(currentColorHex) : 0xFFFF3D00;
        
        android.widget.LinearLayout.LayoutParams previewParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) (60 * getResources().getDisplayMetrics().density));
        previewParams.setMargins(0, 0, 0, padding);
        colorPreview.setLayoutParams(previewParams);
        colorPreview.setBackgroundColor(currentColor);
        layout.addView(colorPreview);

        final android.widget.SeekBar seekR = createColorSeekBar(android.graphics.Color.red(currentColor));
        final android.widget.SeekBar seekG = createColorSeekBar(android.graphics.Color.green(currentColor));
        final android.widget.SeekBar seekB = createColorSeekBar(android.graphics.Color.blue(currentColor));

        layout.addView(createLabel("Red"));
        layout.addView(seekR);
        layout.addView(createLabel("Green"));
        layout.addView(seekG);
        layout.addView(createLabel("Blue"));
        layout.addView(seekB);

        android.widget.SeekBar.OnSeekBarChangeListener listener = new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int color = android.graphics.Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                colorPreview.setBackgroundColor(color);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        };

        seekR.setOnSeekBarChangeListener(listener);
        seekG.setOnSeekBarChangeListener(listener);
        seekB.setOnSeekBarChangeListener(listener);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Compose Custom Color")
                .setView(layout)
                .setPositiveButton("Apply", (dialog, which) -> {
                    int color = android.graphics.Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                    dbHelper.setGlobalSetting("theme_color", String.format("#%08X", color));
                    applyThemeSettings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private android.widget.TextView createLabel(String text) {
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(text);
        tv.setPadding(0, 8, 0, 0);
        return tv;
    }

    private android.widget.SeekBar createColorSeekBar(int progress) {
        android.widget.SeekBar sb = new android.widget.SeekBar(this);
        sb.setMax(255);
        sb.setProgress(progress);
        return sb;
    }

    private void applyThemeSettings() {
        String tPath = dbHelper.getGlobalSetting("toolbar_bg_path");
        String tColor = dbHelper.getGlobalSetting("toolbar_bg_color");
        String fPath = dbHelper.getGlobalSetting("fab_bg_path");
        String fColor = dbHelper.getGlobalSetting("fab_bg_color");
        String themeColorHex = dbHelper.getGlobalSetting("theme_color");
        
        int color;
        if (themeColorHex != null) {
            color = android.graphics.Color.parseColor(themeColorHex);
        } else {
            color = getResources().getColor(R.color.primary);
        }

        if (toolbarBackgroundOverlay != null) {
            toolbarBackgroundOverlay.setBackgroundColor(android.graphics.Color.WHITE);
        }
        if (fabBackgroundOverlay != null) {
            fabBackgroundOverlay.setBackgroundColor(android.graphics.Color.WHITE);
        }

        // Toolbar
        if ((tPath == null || tPath.isEmpty()) && (tColor == null || tColor.isEmpty())) {
            if (toolbar != null) {
                toolbar.setBackgroundColor(color);
                toolbar.setAlpha(1.0f);
            }
        }

        // FAB
        if (fab != null) {
            fab.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            fab.setAlpha(1.0f);
            if ((fPath == null || fPath.isEmpty()) && (fColor == null || fColor.isEmpty())) {
                fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                getWindow().setStatusBarColor(color);
            }
        }
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
        boolean toVisitSelected = selectedTags.contains("do zwiedzenia");

        filteredEntries.clear();

        for (TravelEntry entry : allEntries) {
            boolean hasToVisitTag = entry.getTags() != null && entry.getTags().contains("do zwiedzenia");

            // Jeśli wpis ma tag "do zwiedzenia", pokaż go tylko gdy ten tag jest zaznaczony w filtrach
            if (hasToVisitTag && !toVisitSelected) {
                continue;
            }

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
            dbHelper = DatabaseHelper.getInstance(this);
            loadEntries();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}