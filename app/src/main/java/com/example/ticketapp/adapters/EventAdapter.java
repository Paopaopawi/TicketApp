package com.example.ticketapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.activities.AddEditEventActivity;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.models.TicketFormData;
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.firebase.database.DatabaseReference;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final Context context;
    private final List<Event> eventList;

    public EventAdapter(Context context, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {

        Event event = eventList.get(position);
        if (event == null) return;

        holder.tvEventName.setText(event.getTitle() != null ? event.getTitle() : "Untitled");
        holder.tvEventDate.setText("Date: " + (event.getDate() != null ? event.getDate() : "-"));
        holder.tvEventVenue.setText("Venue: " + (event.getLocation() != null ? event.getLocation() : "-"));

        int ticketTypes = event.getTickets() != null ? event.getTickets().size() : 0;
        holder.tvTickets.setText("Ticket types: " + ticketTypes);

        // DELETE button
        holder.tvDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete \"" + (event.getTitle() != null ? event.getTitle() : "this event") + "\"?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseReference eventRef = FirebaseHelper.getEventsRef().child(event.getEventId());
                        eventRef.removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // EDIT button
        holder.tvEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditEventActivity.class);
            intent.putExtra("eventId", event.getEventId());
            context.startActivity(intent);
        });

        // CLICK CARD -> show details popup with ticket breakdown and total remaining
        holder.itemView.setOnClickListener(v -> {
            showEventDetailsDialog(event);
        });
    }

    private void showEventDetailsDialog(Event event) {
        StringBuilder sb = new StringBuilder();
        int totalRemaining = 0;

        List<TicketFormData> tickets = event.getTickets();
        if (tickets != null && !tickets.isEmpty()) {
            for (TicketFormData t : tickets) {
                // prefer readable name: use getName() or getTypeName()
                String label = (t.getName() != null && !t.getName().isEmpty()) ? t.getName() : t.getTypeName();
                int qty = t.getQuantity();
                sb.append(label).append(": ").append(qty).append("\n");
                totalRemaining += qty;
            }
        } else {
            sb.append("No ticket types defined.\n");
        }

        sb.append("\nTotal remaining tickets: ").append(totalRemaining);

        String message = "Where: " + (event.getLocation() != null ? event.getLocation() : "-")
                + "\nWhen: " + (event.getDate() != null ? event.getDate() : "-")
                + "\n\n" + sb.toString();

        new AlertDialog.Builder(context)
                .setTitle(event.getTitle() != null ? event.getTitle() : "Event Details")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView tvEventName, tvEventDate, tvEventVenue, tvTickets, tvEdit, tvDelete;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventVenue = itemView.findViewById(R.id.tvEventVenue);
            tvTickets = itemView.findViewById(R.id.tvTickets);
            tvEdit = itemView.findViewById(R.id.tvEdit);
            tvDelete = itemView.findViewById(R.id.tvDelete);
        }
    }
}
