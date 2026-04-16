package com.traveltracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.traveltracker.R;
import com.traveltracker.database.TravelEntry;

import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.ViewHolder> {

    private List<TravelEntry> entries;
    private OnEntryClickListener listener;

    public interface OnEntryClickListener {
        void onEntryClick(TravelEntry entry);
        void onEntryLongClick(TravelEntry entry);
    }

    public EntryAdapter(List<TravelEntry> entries, OnEntryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TravelEntry entry = entries.get(position);
        holder.titleTextView.setText(entry.getTitle());

        // Wyświetl liczbę notatek, zdjęć i pinezek
        int notesCount = entry.getNotes() != null ? entry.getNotes().size() : 0;
        int photosCount = entry.getPhotos() != null ? entry.getPhotos().size() : 0;
        int pinsCount = entry.getMapPins() != null ? entry.getMapPins().size() : 0;

        holder.infoTextView.setText("📝 " + notesCount + " · 📷 " + photosCount + " · 📍 " + pinsCount);

        holder.itemView.setOnClickListener(v -> listener.onEntryClick(entry));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onEntryLongClick(entry);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView infoTextView;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.entry_title);
            infoTextView = itemView.findViewById(R.id.entry_info);
        }
    }
}