package com.traveltracker.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.traveltracker.R;
import com.traveltracker.database.TravelEntry;

import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.ViewHolder> {

    private List<TravelEntry> entries;
    private OnEntryClickListener listener;
    private ItemTouchHelper itemTouchHelper;
    private int itemColor = android.graphics.Color.WHITE;
    private float itemOpacity = 1.0f;
    private int fontColor = android.graphics.Color.BLACK;

    public interface OnEntryClickListener {
        void onEntryClick(TravelEntry entry);
        void onEntryLongClick(TravelEntry entry);
    }

    public EntryAdapter(List<TravelEntry> entries, OnEntryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    public void setItemStyle(int color, float opacity, int fontColor) {
        this.itemColor = color;
        this.itemOpacity = opacity;
        this.fontColor = fontColor;
        notifyDataSetChanged();
    }

    public void updateEntries(List<TravelEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TravelEntry entry = entries.get(position);
        holder.titleTextView.setText(entry.getTitle());
        holder.titleTextView.setTextColor(fontColor);

        // Aplikuj styl kafelka
        if (holder.itemView instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) holder.itemView;
            
            // Oblicz kolor z uwzględnieniem opacity (tylko dla tła)
            int alpha = Math.round(itemOpacity * 255);
            int colorWithAlpha = android.graphics.Color.argb(
                    alpha,
                    android.graphics.Color.red(itemColor),
                    android.graphics.Color.green(itemColor),
                    android.graphics.Color.blue(itemColor)
            );
            
            cardView.setCardBackgroundColor(colorWithAlpha);
            cardView.setAlpha(1.0f); // Upewnij się, że tekst i ikony są w pełni widoczne
        }

        // Wyświetl liczbę notatek, zdjęć i pinezek
        int notesCount = entry.getNotes() != null ? entry.getNotes().size() : 0;
        int photosCount = entry.getPhotos() != null ? entry.getPhotos().size() : 0;
        int pinsCount = entry.getMapPins() != null ? entry.getMapPins().size() : 0;

        holder.infoTextView.setText("📝 " + notesCount + " · 📷 " + photosCount + " · 📍 " + pinsCount);
        holder.infoTextView.setTextColor(fontColor);
        if (fontColor == android.graphics.Color.BLACK) {
            holder.infoTextView.setAlpha(0.6f);
        } else {
            holder.infoTextView.setAlpha(0.8f);
        }

        // Wyświetl tagi
        if (entry.getTags() != null && !entry.getTags().isEmpty()) {
            holder.tagsTextView.setVisibility(View.VISIBLE);
            holder.tagsTextView.setText("🏷️ " + String.join(", ", entry.getTags()));
            holder.tagsTextView.setTextColor(fontColor);
        } else {
            holder.tagsTextView.setVisibility(View.GONE);
        }

        holder.dragHandle.setColorFilter(fontColor);
        holder.dragHandle.setAlpha(0.6f);

        holder.itemView.setOnClickListener(v -> listener.onEntryClick(entry));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onEntryLongClick(entry);
            return true;
        });

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && itemTouchHelper != null) {
                itemTouchHelper.startDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView infoTextView;
        TextView tagsTextView;
        ImageView dragHandle;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.entry_title);
            infoTextView = itemView.findViewById(R.id.entry_info);
            tagsTextView = itemView.findViewById(R.id.entry_tags);
            dragHandle = itemView.findViewById(R.id.iv_drag_handle);
        }
    }
}