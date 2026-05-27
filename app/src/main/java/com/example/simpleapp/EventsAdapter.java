package com.example.simpleapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {
    private List<CalendarEvent> allEvents;
    private List<CalendarEvent> displayedEvents;
    private String currentDate;

    public EventsAdapter(List<CalendarEvent> events, String currentDate) {
        this.allEvents = events;
        this.currentDate = currentDate;
        this.displayedEvents = filterEventsByDate(events, currentDate);
    }

    private List<CalendarEvent> filterEventsByDate(List<CalendarEvent> events, String date) {
        List<CalendarEvent> filtered = new ArrayList<>();
        for (CalendarEvent event : events) {
            if (event.getDate().equals(date)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    public void updateEvents(List<CalendarEvent> events, String newDate) {
        this.allEvents = events;
        this.currentDate = newDate;
        this.displayedEvents = filterEventsByDate(events, newDate);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(displayedEvents.get(position));
    }

    @Override
    public int getItemCount() {
        return displayedEvents.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        EventViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }

        void bind(CalendarEvent event) {
            textView.setText("• " + event.getTitle());
        }
    }
}