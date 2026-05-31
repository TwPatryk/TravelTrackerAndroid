    package com.traveltracker;
    
    import android.app.Activity;
    import android.content.Intent;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.graphics.Matrix;
    import android.graphics.drawable.Drawable;
    import android.net.Uri;
    import android.util.Log;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.ImageView;
    import android.widget.LinearLayout;
    import android.widget.RadioButton;
    import android.widget.RadioGroup;
    import android.widget.SeekBar;
    import android.widget.TextView;
    
    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AlertDialog;
    import androidx.core.content.ContextCompat;
    
    import com.bumptech.glide.Glide;
    import com.bumptech.glide.load.engine.DiskCacheStrategy;
    import com.bumptech.glide.request.RequestListener;
    import com.bumptech.glide.request.RequestOptions;
    import com.bumptech.glide.request.target.Target;
    import com.traveltracker.adapter.EntryAdapter;
    import com.traveltracker.database.DatabaseHelper;
    
    import java.util.HashMap;
    import java.util.Map;
    
    public class UiSettingsManager {
        private final Activity activity;
        private final DatabaseHelper dbHelper;
        private final UiSettingsListener listener;
    
        private ImageView backgroundImageView;
        private ImageView toolbarBackgroundImageView;
        private View mainBackgroundOverlay;
        private View toolbarBackgroundOverlay;
        private ImageView fabBackgroundImageView;
        private View fabBackgroundOverlay;
        private androidx.appcompat.widget.Toolbar toolbar;
        private com.google.android.material.floatingactionbutton.FloatingActionButton fab;
        private EntryAdapter adapter;
    
        public interface UiSettingsListener {
            void onImagePickRequested(String prefix);
        }
    
        public UiSettingsManager(Activity activity, DatabaseHelper dbHelper, UiSettingsListener listener) {
            this.activity = activity;
            this.dbHelper = dbHelper;
            this.listener = listener;
        }
    
        public void setViews(ImageView backgroundImageView, ImageView toolbarBackgroundImageView,
                             View mainBackgroundOverlay, View toolbarBackgroundOverlay,
                             ImageView fabBackgroundImageView, View fabBackgroundOverlay,
                             androidx.appcompat.widget.Toolbar toolbar,
                             com.google.android.material.floatingactionbutton.FloatingActionButton fab,
                             EntryAdapter adapter) {
            this.backgroundImageView = backgroundImageView;
            this.toolbarBackgroundImageView = toolbarBackgroundImageView;
            this.mainBackgroundOverlay = mainBackgroundOverlay;
            this.toolbarBackgroundOverlay = toolbarBackgroundOverlay;
            this.fabBackgroundImageView = fabBackgroundImageView;
            this.fabBackgroundOverlay = fabBackgroundOverlay;
            this.toolbar = toolbar;
            this.fab = fab;
            this.adapter = adapter;
        }
    
        public void applyBackgroundSettings() {
            new Thread(() -> {
                final Map<String, String> settings = new HashMap<>();
                String[] keys = {"theme_color",
                        "main_bg_path", "main_bg_color", "main_bg_opacity", "main_bg_scale_type", "main_bg_offset_x", "main_bg_offset_y",
                        "toolbar_bg_path", "toolbar_bg_color", "toolbar_bg_opacity", "toolbar_bg_scale_type", "toolbar_bg_offset_x", "toolbar_bg_offset_y",
                        "fab_bg_path", "fab_bg_color", "fab_bg_opacity", "fab_bg_scale_type", "fab_bg_offset_x", "fab_bg_offset_y",
                        "item_bg_color", "item_bg_opacity", "item_font_color", "toolbar_content_color"};
                for (String key : keys) {
                    settings.put(key, dbHelper.getGlobalSetting(key));
                }
                activity.runOnUiThread(() -> applyBackgroundSettingsFromMap(settings));
            }).start();
        }
    
        private void applyBackgroundSettingsFromMap(Map<String, String> settings) {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                int primaryColor = ContextCompat.getColor(activity, R.color.primary);
                int themeColor = safeParseColor(settings.get("theme_color"), primaryColor);
    
                RequestOptions bgOptions = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(800)
                        .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.AT_MOST)
                        .error(android.R.drawable.ic_menu_report_image);
    
                // 1. Main Background
                String path = settings.get("main_bg_path");
                String colorHex = settings.get("main_bg_color");
                String opacityStr = settings.get("main_bg_opacity");
                String scaleTypeStr = settings.get("main_bg_scale_type");
                float offsetX = safeParseFloat(settings.get("main_bg_offset_x"), 0.5f);
                float offsetY = safeParseFloat(settings.get("main_bg_offset_y"), 0.5f);
    
                if (path != null && !path.isEmpty()) {
                    Uri uri = Uri.parse(path);
                    try {
                        activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
    
                    com.bumptech.glide.RequestBuilder<Drawable> requestBuilder =
                            Glide.with(activity).load(uri).apply(bgOptions);
    
                    ImageView.ScaleType st = getScaleType(scaleTypeStr);
                    backgroundImageView.setVisibility(View.VISIBLE);
                    backgroundImageView.setScaleType(st);
    
                    if (st == ImageView.ScaleType.MATRIX) {
                        backgroundImageView.setScaleType(ImageView.ScaleType.MATRIX);
    
                        requestBuilder = requestBuilder.listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }
    
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                backgroundImageView.post(() -> {
                                    Matrix matrix = new Matrix();
                                    float viewWidth = backgroundImageView.getWidth();
                                    float viewHeight = backgroundImageView.getHeight();
                                    if (viewWidth > 0 && viewHeight > 0) {
                                        float drawableWidth = resource.getIntrinsicWidth();
                                        float drawableHeight = resource.getIntrinsicHeight();
                                        float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);
                                        matrix.setScale(scale, scale);
                                        float finalOffsetX = (viewWidth - drawableWidth * scale) * offsetX;
                                        float finalOffsetY = (viewHeight - drawableHeight * scale) * offsetY;
                                        matrix.postTranslate(finalOffsetX, finalOffsetY);
                                        backgroundImageView.setImageMatrix(matrix);
                                    }
                                });
                                return false;
                            }
                        });
    
                        backgroundImageView.post(() -> {
                            Matrix matrix = new Matrix();
                            float viewWidth = backgroundImageView.getWidth();
                            float viewHeight = backgroundImageView.getHeight();
                            if (backgroundImageView.getDrawable() != null && viewWidth > 0 && viewHeight > 0) {
                                float drawableWidth = backgroundImageView.getDrawable().getIntrinsicWidth();
                                float drawableHeight = backgroundImageView.getDrawable().getIntrinsicHeight();
                                float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);
                                matrix.setScale(scale, scale);
                                float finalOffsetX = (viewWidth - drawableWidth * scale) * offsetX;
                                float finalOffsetY = (viewHeight - drawableHeight * scale) * offsetY;
                                matrix.postTranslate(finalOffsetX, finalOffsetY);
                                backgroundImageView.setImageMatrix(matrix);
                            }
                        });
                    }
    
                    requestBuilder.into(backgroundImageView);
    
                    if (mainBackgroundOverlay != null) {
                        mainBackgroundOverlay.setVisibility(View.VISIBLE);
                        float opacity = safeParseFloat(opacityStr, 1.0f);
                        mainBackgroundOverlay.setAlpha(1.0f - opacity);
                        mainBackgroundOverlay.setBackgroundColor(Color.WHITE);
                    }
                } else {
                    backgroundImageView.setVisibility(View.GONE);
                    if (mainBackgroundOverlay != null) {
                        if (colorHex != null && !colorHex.isEmpty()) {
                            mainBackgroundOverlay.setVisibility(View.VISIBLE);
                            mainBackgroundOverlay.setAlpha(1.0f);
                            mainBackgroundOverlay.setBackgroundColor(safeParseColor(colorHex, Color.WHITE));
                        } else {
                            mainBackgroundOverlay.setVisibility(View.GONE);
                        }
                    }
                }
    
                // 2. Toolbar & Status Bar
                String tPath = settings.get("toolbar_bg_path");
                String tColorHex = settings.get("toolbar_bg_color");
                String tScaleTypeStr = settings.get("toolbar_bg_scale_type");
                float tOffsetX = safeParseFloat(settings.get("toolbar_bg_offset_x"), 0.5f);
                float tOffsetY = safeParseFloat(settings.get("toolbar_bg_offset_y"), 0.5f);
                int tColor = (tColorHex != null && !tColorHex.isEmpty()) ? safeParseColor(tColorHex, themeColor) : themeColor;
    
                activity.getWindow().setStatusBarColor(tColor);
                if (toolbar != null) toolbar.setBackgroundColor(Color.TRANSPARENT);
    
                if (tPath != null && !tPath.isEmpty()) {
                    com.bumptech.glide.RequestBuilder<Drawable> tRequestBuilder =
                            Glide.with(activity).load(Uri.parse(tPath)).apply(bgOptions);
    
                    ImageView.ScaleType tSt = getScaleType(tScaleTypeStr);
                    toolbarBackgroundImageView.setVisibility(View.VISIBLE);
                    toolbarBackgroundImageView.setScaleType(tSt);
    
                    if (tSt == ImageView.ScaleType.MATRIX) {
                        toolbarBackgroundImageView.setScaleType(ImageView.ScaleType.MATRIX);
    
                        tRequestBuilder = tRequestBuilder.listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }
    
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                toolbarBackgroundImageView.post(() -> {
                                    Matrix matrix = new Matrix();
                                    float viewWidth = toolbarBackgroundImageView.getWidth();
                                    float viewHeight = toolbarBackgroundImageView.getHeight();
                                    if (viewWidth > 0 && viewHeight > 0) {
                                        float drawableWidth = resource.getIntrinsicWidth();
                                        float drawableHeight = resource.getIntrinsicHeight();
                                        float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);
    
                                        // Wymuszenie możliwości przesuwania poziomego dla toolbara
                                        if (toolbarBackgroundImageView.getId() == R.id.toolbar_background_image && (drawableWidth * scale) <= viewWidth * 1.1f) {
                                            scale = scale * 1.5f;
                                        }
    
                                        matrix.setScale(scale, scale);
                                        float finalOffsetX = (viewWidth - drawableWidth * scale) * tOffsetX;
                                        float finalOffsetY = (viewHeight - drawableHeight * scale) * tOffsetY;
                                        matrix.postTranslate(finalOffsetX, finalOffsetY);
                                        toolbarBackgroundImageView.setImageMatrix(matrix);
                                    }
                                });
                                return false;
                            }
                        });
    
                        toolbarBackgroundImageView.post(() -> {
                            Matrix matrix = new Matrix();
                            float viewWidth = toolbarBackgroundImageView.getWidth();
                            float viewHeight = toolbarBackgroundImageView.getHeight();
                            if (toolbarBackgroundImageView.getDrawable() != null && viewWidth > 0 && viewHeight > 0) {
                                float drawableWidth = toolbarBackgroundImageView.getDrawable().getIntrinsicWidth();
                                float drawableHeight = toolbarBackgroundImageView.getDrawable().getIntrinsicHeight();
                                float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);
    
                                // Wymuszenie możliwości przesuwania poziomego dla toolbara
                                if ((drawableWidth * scale) <= viewWidth * 1.1f) {
                                    scale = scale * 1.5f;
                                }
    
                                matrix.setScale(scale, scale);
                                float finalOffsetX = (viewWidth - drawableWidth * scale) * tOffsetX;
                                float finalOffsetY = (viewHeight - drawableHeight * scale) * tOffsetY;
                                matrix.postTranslate(finalOffsetX, finalOffsetY);
                                toolbarBackgroundImageView.setImageMatrix(matrix);
                            }
                        });
                    }
    
                    tRequestBuilder.into(toolbarBackgroundImageView);
    
                    if (toolbarBackgroundOverlay != null) {
                        toolbarBackgroundOverlay.setVisibility(View.VISIBLE);
                        toolbarBackgroundOverlay.setBackgroundColor(Color.WHITE);
                        float tOpacity = safeParseFloat(settings.get("toolbar_bg_opacity"), 1.0f);
                        toolbarBackgroundOverlay.setAlpha(1.0f - tOpacity);
                    }
                } else {
                    toolbarBackgroundImageView.setVisibility(View.GONE);
                    if (toolbarBackgroundOverlay != null) {
                        toolbarBackgroundOverlay.setVisibility(View.VISIBLE);
                        toolbarBackgroundOverlay.setAlpha(1.0f);
                        toolbarBackgroundOverlay.setBackgroundColor(tColor);
                    }
                }
    
                // Toolbar Content Color
                String tContentColorHex = settings.get("toolbar_content_color");
                int tContentColor = (tContentColorHex != null && !tContentColorHex.isEmpty()) ? safeParseColor(tContentColorHex, Color.WHITE) : Color.WHITE;
    
                TextView toolbarTitle = activity.findViewById(R.id.toolbar_title);
                ImageView menuButton = activity.findViewById(R.id.menu_button);
                ImageView filterButton = activity.findViewById(R.id.filter_button);
                ImageView toolbarLogo = activity.findViewById(R.id.toolbar_logo);
    
                if (toolbarTitle != null) toolbarTitle.setTextColor(tContentColor);
                if (menuButton != null) menuButton.setColorFilter(tContentColor);
                if (filterButton != null) filterButton.setColorFilter(tContentColor);
                if (toolbarLogo != null) toolbarLogo.setColorFilter(tContentColor);
    
                // 3. FAB
                String fColorHex = settings.get("fab_bg_color");
                int fColor = (fColorHex != null && !fColorHex.isEmpty()) ? safeParseColor(fColorHex, themeColor) : themeColor;
                if (fab != null) fab.setBackgroundTintList(ColorStateList.valueOf(fColor));
    
                String fPath = settings.get("fab_bg_path");
                String fScaleTypeStr = settings.get("fab_bg_scale_type");
                float fOffsetX = safeParseFloat(settings.get("fab_bg_offset_x"), 0.5f);
                float fOffsetY = safeParseFloat(settings.get("fab_bg_offset_y"), 0.5f);
    
                if (fPath != null && !fPath.isEmpty()) {
                    com.bumptech.glide.RequestBuilder<Drawable> fRequestBuilder =
                            Glide.with(activity).load(Uri.parse(fPath)).apply(bgOptions);
    
                    ImageView.ScaleType fSt = getScaleType(fScaleTypeStr);
                    fabBackgroundImageView.setVisibility(View.VISIBLE);
                    fabBackgroundImageView.setScaleType(fSt);
    
                    if (fSt == ImageView.ScaleType.MATRIX) {
                        fabBackgroundImageView.setScaleType(ImageView.ScaleType.MATRIX);
    
                        fRequestBuilder = fRequestBuilder.listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }
    
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                fabBackgroundImageView.post(() -> {
                                    Matrix matrix = new Matrix();
                                    float viewWidth = fabBackgroundImageView.getWidth();
                                    float viewHeight = fabBackgroundImageView.getHeight();
                                    if (viewWidth > 0 && viewHeight > 0) {
                                        float drawableWidth = resource.getIntrinsicWidth();
                                        float drawableHeight = resource.getIntrinsicHeight();
                                        float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);
                                        matrix.setScale(scale, scale);
                                        float finalOffsetX = (viewWidth - drawableWidth * scale) * fOffsetX;
                                        float finalOffsetY = (viewHeight - drawableHeight * scale) * fOffsetY;
                                        matrix.postTranslate(finalOffsetX, finalOffsetY);
                                        fabBackgroundImageView.setImageMatrix(matrix);
                                    }
                                });
                                return false;
                            }
                        });
    
                        fabBackgroundImageView.post(() -> {
                            Matrix matrix = new Matrix();
                            float viewWidth = fabBackgroundImageView.getWidth();
                            float viewHeight = fabBackgroundImageView.getHeight();
                            if (fabBackgroundImageView.getDrawable() != null && viewWidth > 0 && viewHeight > 0) {
                                float drawableWidth = fabBackgroundImageView.getDrawable().getIntrinsicWidth();
                                float drawableHeight = fabBackgroundImageView.getDrawable().getIntrinsicHeight();
                                float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);
                                matrix.setScale(scale, scale);
                                float finalOffsetX = (viewWidth - drawableWidth * scale) * fOffsetX;
                                float finalOffsetY = (viewHeight - drawableHeight * scale) * fOffsetY;
                                matrix.postTranslate(finalOffsetX, finalOffsetY);
                                fabBackgroundImageView.setImageMatrix(matrix);
                            }
                        });
                    }
    
                    fRequestBuilder.into(fabBackgroundImageView);
    
                    if (fabBackgroundOverlay != null) {
                        fabBackgroundOverlay.setVisibility(View.VISIBLE);
                        float fOpacity = safeParseFloat(fPath != null ? settings.get("fab_bg_opacity") : "1.0", 1.0f);
                        fabBackgroundOverlay.setAlpha(1.0f - fOpacity);
                        fabBackgroundOverlay.setBackgroundColor(Color.WHITE);
                    }
                } else {
                    fabBackgroundImageView.setVisibility(View.GONE);
                    if (fabBackgroundOverlay != null) {
                        fabBackgroundOverlay.setVisibility(View.GONE);
                    }
                }
    
                // 4. Items Style
                String iColorHex = settings.get("item_bg_color");
                String iOpacityStr = settings.get("item_bg_opacity");
                String iFontColorHex = settings.get("item_font_color");
                Log.d("UiSettingsManager", "Loading items style: color=" + iColorHex + ", opacity=" + iOpacityStr + ", font=" + iFontColorHex);
    
                int iColor = (iColorHex != null && !iColorHex.isEmpty()) ? safeParseColor(iColorHex, Color.WHITE) : Color.WHITE;
                int iFontColor = (iFontColorHex != null && !iFontColorHex.isEmpty()) ? safeParseColor(iFontColorHex, Color.BLACK) : Color.BLACK;
                float iOpacity = safeParseFloat(iOpacityStr, 1.0f);
                if (adapter != null) {
                    adapter.setItemStyle(iColor, iOpacity, iFontColor);
                }
    
            } catch (Exception e) {
                Log.e("UiSettingsManager", "Error applying background settings", e);
            }
        }
    
        public void showBackgroundSettingsDialog(final String prefix) {
            final boolean[] isCleared = {false};
            View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_background_settings, null);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(dialogView)
                    .create();
    
            TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
            if ("toolbar_bg_".equals(prefix)) tvTitle.setText("Toolbar Background");
            else if ("fab_bg_".equals(prefix)) tvTitle.setText("FAB Background");
            else if ("item_bg_".equals(prefix)) tvTitle.setText("Items Style");
            else tvTitle.setText("Main Background");
    
            SeekBar seekBar = dialogView.findViewById(R.id.seekbar_opacity);
            RadioGroup rgScaleType = dialogView.findViewById(R.id.rg_scale_type);
            SeekBar seekOffsetX = dialogView.findViewById(R.id.seekbar_offset_x);
            SeekBar seekOffsetY = dialogView.findViewById(R.id.seekbar_offset_y);
            View layoutOffsets = dialogView.findViewById(R.id.layout_offsets);
            Button btnSavePosition = dialogView.findViewById(R.id.btn_save_position);
    
            if ("toolbar_bg_".equals(prefix)) {
                btnSavePosition.setVisibility(View.VISIBLE);
                btnSavePosition.setOnClickListener(v -> {
                    float ox = seekOffsetX.getProgress() / 100f;
                    float oy = seekOffsetY.getProgress() / 100f;
                    dbHelper.setGlobalSetting(prefix + "offset_x", String.valueOf(ox));
                    dbHelper.setGlobalSetting(prefix + "offset_y", String.valueOf(oy));
                    dbHelper.setGlobalSetting(prefix + "scale_type", "CENTER_CROP_CUSTOM");
                    applyBackgroundSettings();
                });
            }
    
            Button btnSelect = dialogView.findViewById(R.id.btn_select_image);
            Button btnSelectColor = dialogView.findViewById(R.id.btn_select_color);
            Button btnClear = dialogView.findViewById(R.id.btn_clear_bg);
    
            btnSelect.setTextColor(Color.BLACK);
            btnSelectColor.setTextColor(Color.BLACK);
            btnClear.setTextColor(Color.BLACK);
    
            String currentOpacity = dbHelper.getGlobalSetting(prefix + "opacity");
            seekBar.setProgress((int) ((currentOpacity != null ? Float.parseFloat(currentOpacity) : 1.0f) * 100));
    
            String currentOffsetX = dbHelper.getGlobalSetting(prefix + "offset_x");
            seekOffsetX.setProgress((int) ((currentOffsetX != null ? Float.parseFloat(currentOffsetX) : 0.5f) * 100));
            String currentOffsetY = dbHelper.getGlobalSetting(prefix + "offset_y");
            seekOffsetY.setProgress((int) ((currentOffsetY != null ? Float.parseFloat(currentOffsetY) : 0.5f) * 100));
    
            String currentScale = dbHelper.getGlobalSetting(prefix + "scale_type");
            
            // Logika wymuszająca Stretch to Fill (FIT_XY) jako startowy wybór
            rgScaleType.setOnCheckedChangeListener(null); // Tymczasowo usuń listener
            rgScaleType.check(R.id.rb_fit_xy);
            if (layoutOffsets != null) layoutOffsets.setVisibility(View.GONE);

            if ("FIT_CENTER".equals(currentScale)) {
                rgScaleType.check(R.id.rb_fit_center);
            } else if ("CENTER_CROP_CUSTOM".equals(currentScale)) {
                rgScaleType.check(R.id.rb_center_crop);
                if (layoutOffsets != null) layoutOffsets.setVisibility(View.VISIBLE);
            } else {
                // Jeśli null, CENTER_CROP lub MATRIX - zostawiamy FIT_XY i naprawiamy bazę
                dbHelper.setGlobalSetting(prefix + "scale_type", "FIT_XY");
            }
    
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float opacity = progress / 100f;
                        View overlay = "toolbar_bg_".equals(prefix) ? toolbarBackgroundOverlay :
                                ("fab_bg_".equals(prefix) ? fabBackgroundOverlay : mainBackgroundOverlay);
                        if (overlay != null) {
                            overlay.setAlpha(1.0f - opacity);
                        }
                        // Save immediately to DB for better persistence during live preview
                        dbHelper.setGlobalSetting(prefix + "opacity", String.valueOf(opacity));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekOffsetX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float ox = progress / 100f;
                        float oy = seekOffsetY.getProgress() / 100f;

                        Log.d("UiSettingsManager", "Offset changed: X=" + ox + ", Y=" + oy + " for prefix=" + prefix);

                        // Zapisz do bazy
                        dbHelper.setGlobalSetting(prefix + "offset_x", String.valueOf(ox));
                        dbHelper.setGlobalSetting(prefix + "offset_y", String.valueOf(oy));

                        // Zaktualizuj widok BEZPOŚREDNIO dla toolbara
                        if ("toolbar_bg_".equals(prefix)) {
                            Log.d("UiSettingsManager", "Updating toolbar matrix");
                            updateToolbarImageMatrix(ox, oy);
                        } else {
                            ImageView targetIv = ("fab_bg_".equals(prefix) ? fabBackgroundImageView : backgroundImageView);
                            updateImageViewMatrix(targetIv, ox, oy);
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekOffsetY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float ox = seekOffsetX.getProgress() / 100f;
                        float oy = progress / 100f;

                        // Zapisz do bazy
                        dbHelper.setGlobalSetting(prefix + "offset_x", String.valueOf(ox));
                        dbHelper.setGlobalSetting(prefix + "offset_y", String.valueOf(oy));

                        // Zaktualizuj widok BEZPOŚREDNIO dla toolbara
                        if ("toolbar_bg_".equals(prefix)) {
                            updateToolbarImageMatrix(ox, oy);
                        } else {
                            ImageView targetIv = ("fab_bg_".equals(prefix) ? fabBackgroundImageView : backgroundImageView);
                            updateImageViewMatrix(targetIv, ox, oy);
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
    
            rgScaleType.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_center_crop) {
                    if (layoutOffsets != null) layoutOffsets.setVisibility(View.VISIBLE);
                } else {
                    if (layoutOffsets != null) layoutOffsets.setVisibility(View.GONE);
                }
    
                ImageView targetIv = "toolbar_bg_".equals(prefix) ? toolbarBackgroundImageView :
                        ("fab_bg_".equals(prefix) ? fabBackgroundImageView : backgroundImageView);
    
                ImageView.ScaleType st = getScaleTypeByCheckedId(checkedId);
                targetIv.setScaleType(st);
                
                String selectedScale;
                if (checkedId == R.id.rb_fit_center) selectedScale = "FIT_CENTER";
                else if (checkedId == R.id.rb_fit_xy) selectedScale = "FIT_XY";
                else selectedScale = "CENTER_CROP_CUSTOM";
                
                dbHelper.setGlobalSetting(prefix + "scale_type", selectedScale);

                if (st == ImageView.ScaleType.MATRIX) {
                    updateImageViewMatrix(targetIv, seekOffsetX.getProgress() / 100f, seekOffsetY.getProgress() / 100f);
                }
            });
    
            if ("item_bg_".equals(prefix)) {
                rgScaleType.setVisibility(View.GONE);
                btnSelect.setVisibility(View.GONE);
                if (layoutOffsets != null) layoutOffsets.setVisibility(View.GONE);
    
                TextView tvFontTitle = new TextView(activity);
                tvFontTitle.setText("Font Color");
                tvFontTitle.setPadding(0, (int)(16 * activity.getResources().getDisplayMetrics().density), 0, 0);
                ((ViewGroup)dialogView).addView(tvFontTitle, ((ViewGroup)dialogView).indexOfChild(btnClear));
    
                RadioGroup rgFontColor = new RadioGroup(activity);
                rgFontColor.setOrientation(RadioGroup.HORIZONTAL);
    
                RadioButton rbBlack = new RadioButton(activity);
                rbBlack.setText("Black");
                rbBlack.setId(View.generateViewId());
    
                RadioButton rbWhite = new RadioButton(activity);
                rbWhite.setText("White");
                rbWhite.setId(View.generateViewId());
    
                rgFontColor.addView(rbBlack);
                rgFontColor.addView(rbWhite);
    
                String currentFontColor = dbHelper.getGlobalSetting("item_font_color");
                if ("#FFFFFFFF".equalsIgnoreCase(currentFontColor)) rgFontColor.check(rbWhite.getId());
                else rgFontColor.check(rbBlack.getId());
    
                ((ViewGroup)dialogView).addView(rgFontColor, ((ViewGroup)dialogView).indexOfChild(btnClear));
    
                dialog.setOnDismissListener(d -> {
                    if (isCleared[0]) return;
                    float opacity = seekBar.getProgress() / 100f;
                    dbHelper.setGlobalSetting(prefix + "opacity", String.valueOf(opacity));
    
                    int checkedFontId = rgFontColor.getCheckedRadioButtonId();
                    if (checkedFontId == rbWhite.getId()) dbHelper.setGlobalSetting("item_font_color", "#FFFFFFFF");
                    else if (checkedFontId == rbBlack.getId()) dbHelper.setGlobalSetting("item_font_color", "#FF000000");
    
                    applyBackgroundSettings();
                });
            } else if ("toolbar_bg_".equals(prefix)) {
                TextView tvContentTitle = new TextView(activity);
                tvContentTitle.setText("Content Color (Title & Icons)");
                tvContentTitle.setPadding(0, (int)(16 * activity.getResources().getDisplayMetrics().density), 0, 0);
                ((ViewGroup)dialogView).addView(tvContentTitle, ((ViewGroup)dialogView).indexOfChild(btnClear));
    
                RadioGroup rgContentColor = new RadioGroup(activity);
                rgContentColor.setOrientation(RadioGroup.HORIZONTAL);
    
                RadioButton rbBlack = new RadioButton(activity);
                rbBlack.setText("Black");
                rbBlack.setId(View.generateViewId());
    
                RadioButton rbWhite = new RadioButton(activity);
                rbWhite.setText("White");
                rbWhite.setId(View.generateViewId());
    
                rgContentColor.addView(rbBlack);
                rgContentColor.addView(rbWhite);
    
                String currentContentColor = dbHelper.getGlobalSetting("toolbar_content_color");
                if ("#FF000000".equalsIgnoreCase(currentContentColor)) rgContentColor.check(rbBlack.getId());
                else rgContentColor.check(rbWhite.getId());
    
                ((ViewGroup)dialogView).addView(rgContentColor, ((ViewGroup)dialogView).indexOfChild(btnClear));
    
                dialog.setOnDismissListener(d -> {
                    if (isCleared[0]) return;
                    float opacity = seekBar.getProgress() / 100f;
                    dbHelper.setGlobalSetting(prefix + "opacity", String.valueOf(opacity));
    
                    int checkedId = rgScaleType.getCheckedRadioButtonId();
                    String selectedScale = "FIT_XY"; // Default
                    if (checkedId == R.id.rb_fit_center) selectedScale = "FIT_CENTER";
                    else if (checkedId == R.id.rb_center_crop) selectedScale = "CENTER_CROP_CUSTOM";
                    dbHelper.setGlobalSetting(prefix + "scale_type", selectedScale);
    
                    int checkedContentId = rgContentColor.getCheckedRadioButtonId();
                    if (checkedContentId == rbWhite.getId()) dbHelper.setGlobalSetting("toolbar_content_color", "#FFFFFFFF");
                    else if (checkedContentId == rbBlack.getId()) dbHelper.setGlobalSetting("toolbar_content_color", "#FF000000");
    
                    applyBackgroundSettings();
                });
            } else if ("fab_bg_".equals(prefix)) {
                dialog.setOnDismissListener(d -> {
                    if (isCleared[0]) return;
                    float opacity = seekBar.getProgress() / 100f;
                    dbHelper.setGlobalSetting(prefix + "opacity", String.valueOf(opacity));
    
                    int checkedId = rgScaleType.getCheckedRadioButtonId();
                    String selectedScale = "FIT_XY"; // Nowy domyślny
                    if (checkedId == R.id.rb_fit_center) selectedScale = "FIT_CENTER";
                    else if (checkedId == R.id.rb_center_crop) selectedScale = "CENTER_CROP_CUSTOM";
                    
                    dbHelper.setGlobalSetting(prefix + "scale_type", selectedScale);
                    applyBackgroundSettings();
                });
            } else {
                dialog.setOnDismissListener(d -> {
                    if (isCleared[0]) return;
                    float opacity = seekBar.getProgress() / 100f;
                    dbHelper.setGlobalSetting(prefix + "opacity", String.valueOf(opacity));
    
                    int checkedId = rgScaleType.getCheckedRadioButtonId();
                    String selectedScale = "FIT_XY"; // Default
                    if (checkedId == R.id.rb_fit_center) selectedScale = "FIT_CENTER";
                    else if (checkedId == R.id.rb_center_crop) selectedScale = "CENTER_CROP_CUSTOM";
                    
                    dbHelper.setGlobalSetting(prefix + "scale_type", selectedScale);
    
                    applyBackgroundSettings();
                });
            }
    
            btnSelect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImagePickRequested(prefix);
                }
                dialog.dismiss();
            });
    
            btnSelectColor.setOnClickListener(v -> {
                showColorChooserForPrefix(prefix);
                dialog.dismiss();
            });
    
            btnClear.setOnClickListener(v -> {
                isCleared[0] = true;
                dbHelper.setGlobalSetting(prefix + "path", null);
                dbHelper.setGlobalSetting(prefix + "color", null);
                dbHelper.setGlobalSetting(prefix + "opacity", "1.0");
                dbHelper.setGlobalSetting(prefix + "offset_x", "0.5");
                dbHelper.setGlobalSetting(prefix + "offset_y", "0.5");
                if ("item_bg_".equals(prefix)) {
                    dbHelper.setGlobalSetting("item_font_color", "#FF000000");
                } else if ("toolbar_bg_".equals(prefix)) {
                    dbHelper.setGlobalSetting("toolbar_content_color", "#FFFFFFFF");
                    dbHelper.setGlobalSetting(prefix + "scale_type", "FIT_XY");
                } else {
                    dbHelper.setGlobalSetting(prefix + "scale_type", "FIT_XY");
                }
                applyBackgroundSettings();
                dialog.dismiss();
            });
    
            dialog.show();
        }
    
        private ImageView.ScaleType getScaleTypeByCheckedId(int checkedId) {
            if (checkedId == R.id.rb_fit_center) return ImageView.ScaleType.FIT_CENTER;
            if (checkedId == R.id.rb_fit_xy) return ImageView.ScaleType.FIT_XY;
            return ImageView.ScaleType.MATRIX;
        }

        private void updateToolbarImageMatrix(float offsetX, float offsetY) {
            if (toolbarBackgroundImageView == null || toolbarBackgroundImageView.getVisibility() != View.VISIBLE) return;

            toolbarBackgroundImageView.post(() -> {
                Drawable drawable = toolbarBackgroundImageView.getDrawable();
                if (drawable == null) return;

                toolbarBackgroundImageView.setScaleType(ImageView.ScaleType.MATRIX);
                Matrix matrix = new Matrix();
                float viewWidth = toolbarBackgroundImageView.getWidth();
                float viewHeight = toolbarBackgroundImageView.getHeight();

                if (viewWidth > 0 && viewHeight > 0) {
                    float drawableWidth = drawable.getIntrinsicWidth();
                    float drawableHeight = drawable.getIntrinsicHeight();
                    if (drawableWidth > 0 && drawableHeight > 0) {
                        float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);

                        // Wymuszenie możliwości przesuwania poziomego dla toolbara
                        if ((drawableWidth * scale) <= viewWidth * 1.1f) {
                            scale = scale * 1.5f;
                        }

                        matrix.setScale(scale, scale);
                        float finalOffsetX = (viewWidth - drawableWidth * scale) * offsetX;
                        float finalOffsetY = (viewHeight - drawableHeight * scale) * offsetY;
                        matrix.postTranslate(finalOffsetX, finalOffsetY);
                        toolbarBackgroundImageView.setImageMatrix(matrix);

                        // Wymuszenie odświeżenia
                        toolbarBackgroundImageView.invalidate();
                    }
                }
            });
        }
    
        private void updateImageViewMatrix(ImageView imageView, float offsetX, float offsetY) {
            if (imageView == null || imageView.getVisibility() != View.VISIBLE) return;
            imageView.post(() -> {
                Drawable drawable = imageView.getDrawable();
                if (drawable == null) return;
    
                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                Matrix matrix = new Matrix();
                float viewWidth = imageView.getWidth();
                float viewHeight = imageView.getHeight();
    
                if (viewWidth > 0 && viewHeight > 0) {
                    float drawableWidth = drawable.getIntrinsicWidth();
                    float drawableHeight = drawable.getIntrinsicHeight();
                    if (drawableWidth > 0 && drawableHeight > 0) {
                        float scale = Math.max(viewWidth / drawableWidth, viewHeight / drawableHeight);
    
                        // Wymuszenie możliwości przesuwania poziomego dla tła toolbara
                        if (imageView.getId() == R.id.toolbar_background_image) {
                            if ((drawableWidth * scale) <= viewWidth * 1.1f) {
                                scale = scale * 1.5f;
                            }
                        }
    
                        matrix.setScale(scale, scale);
                        float finalOffsetX = (viewWidth - drawableWidth * scale) * offsetX;
                        float finalOffsetY = (viewHeight - drawableHeight * scale) * offsetY;
                        matrix.postTranslate(finalOffsetX, finalOffsetY);
                        imageView.setImageMatrix(matrix);
                    }
                }
            });
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
    
            new AlertDialog.Builder(activity)
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
            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            int padding = (int) (16 * activity.getResources().getDisplayMetrics().density);
            layout.setPadding(padding, padding, padding, padding);
    
            final View colorPreview = new View(activity);
            String currentColorHex = dbHelper.getGlobalSetting(prefix + "color");
            int currentColor = safeParseColor(currentColorHex, 0xFFFF3D00);
    
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (60 * activity.getResources().getDisplayMetrics().density));
            previewParams.setMargins(0, 0, 0, padding);
            colorPreview.setLayoutParams(previewParams);
            colorPreview.setBackgroundColor(currentColor);
            layout.addView(colorPreview);
    
            final SeekBar seekR = createColorSeekBar(Color.red(currentColor));
            final SeekBar seekG = createColorSeekBar(Color.green(currentColor));
            final SeekBar seekB = createColorSeekBar(Color.blue(currentColor));
    
            layout.addView(createLabel("Red"));
            layout.addView(seekR);
            layout.addView(createLabel("Green"));
            layout.addView(seekG);
            layout.addView(createLabel("Blue"));
            layout.addView(seekB);
    
            SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int color = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                    colorPreview.setBackgroundColor(color);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            };
    
            seekR.setOnSeekBarChangeListener(listener);
            seekG.setOnSeekBarChangeListener(listener);
            seekB.setOnSeekBarChangeListener(listener);
    
            new AlertDialog.Builder(activity)
                    .setTitle("Compose Custom Color")
                    .setView(layout)
                    .setPositiveButton("Apply", (dialog, which) -> {
                        int color = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                        dbHelper.setGlobalSetting(prefix + "color", String.format("#%08X", color));
                        dbHelper.setGlobalSetting(prefix + "path", null); // Clear path if color chosen
                        applyBackgroundSettings();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    
        public void showThemeChooserDialog() {
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
    
            new AlertDialog.Builder(activity)
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
            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            int padding = (int) (16 * activity.getResources().getDisplayMetrics().density);
            layout.setPadding(padding, padding, padding, padding);
    
            final View colorPreview = new View(activity);
            String currentColorHex = dbHelper.getGlobalSetting("theme_color");
            int currentColor = safeParseColor(currentColorHex, 0xFFFF3D00);
    
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (60 * activity.getResources().getDisplayMetrics().density));
            previewParams.setMargins(0, 0, 0, padding);
            colorPreview.setLayoutParams(previewParams);
            colorPreview.setBackgroundColor(currentColor);
            layout.addView(colorPreview);
    
            final SeekBar seekR = createColorSeekBar(Color.red(currentColor));
            final SeekBar seekG = createColorSeekBar(Color.green(currentColor));
            final SeekBar seekB = createColorSeekBar(Color.blue(currentColor));
    
            layout.addView(createLabel("Red"));
            layout.addView(seekR);
            layout.addView(createLabel("Green"));
            layout.addView(seekG);
            layout.addView(createLabel("Blue"));
            layout.addView(seekB);
    
            SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int color = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                    colorPreview.setBackgroundColor(color);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            };
    
            seekR.setOnSeekBarChangeListener(listener);
            seekG.setOnSeekBarChangeListener(listener);
            seekB.setOnSeekBarChangeListener(listener);
    
            new AlertDialog.Builder(activity)
                    .setTitle("Compose Custom Color")
                    .setView(layout)
                    .setPositiveButton("Apply", (dialog, which) -> {
                        int color = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                        dbHelper.setGlobalSetting("theme_color", String.format("#%08X", color));
                        applyThemeSettings();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    
        private TextView createLabel(String text) {
            TextView tv = new TextView(activity);
            tv.setText(text);
            tv.setPadding(0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0, 0);
            return tv;
        }
    
        private SeekBar createColorSeekBar(int initialProgress) {
            SeekBar sb = new SeekBar(activity);
            sb.setMax(255);
            sb.setProgress(initialProgress);
            return sb;
        }
    
        public void applyThemeSettings() {
            applyBackgroundSettings();
        }
    
        private ImageView.ScaleType getScaleType(String scaleTypeStr) {
        if ("FIT_CENTER".equals(scaleTypeStr)) return ImageView.ScaleType.FIT_CENTER;
        if ("CENTER_CROP_CUSTOM".equals(scaleTypeStr)) return ImageView.ScaleType.MATRIX;
        return ImageView.ScaleType.FIT_XY;
    }
    
        private int safeParseColor(String colorHex, int fallbackColor) {
            if (colorHex == null || colorHex.isEmpty()) return fallbackColor;
            try {
                return Color.parseColor(colorHex);
            } catch (Exception e) {
                return fallbackColor;
            }
        }
    
        private float safeParseFloat(String value, float defaultValue) {
            if (value == null || value.isEmpty()) return defaultValue;
            try {
                return Float.parseFloat(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }
