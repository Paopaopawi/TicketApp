package com.example.ticketapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.activities.TicketBookingActivity;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.models.TicketFormData;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class UserEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<Object> itemList;
    private final boolean isAdmin;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EVENT = 1;

    public UserEventAdapter(Context context, List<Object> itemList, boolean isAdmin) {
        this.context = context;
        this.itemList = itemList;
        this.isAdmin = isAdmin;
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position) instanceof String ? TYPE_HEADER : TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_event_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
            return new EventViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (getItemViewType(position) == TYPE_HEADER) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) itemList.get(position));
            return;
        }

        Event e = (Event) itemList.get(position);
        EventViewHolder evh = (EventViewHolder) holder;

        evh.tvEventName.setText(e.getTitle());
        evh.tvEventDate.setText("Date: " + e.getDate());
        evh.tvEventDetails.setText("Details: " + e.getDetails());

        // Sale Start & End Time (display on card)
        evh.tvSaleStart.setText("Sale Start: " + formatTime(e.getSaleStartTime()));
        evh.tvSaleEnd.setText("Sale End: " + formatTime(e.getSaleEndTime()));

        // Compute total tickets left
        int totalRemaining = 0;
        List<TicketFormData> tickets = e.getTickets();
        if (tickets != null) {
            for (TicketFormData t : tickets) {
                totalRemaining += t.getQuantity();
            }
        }
        evh.tvTickets.setText("Tickets left: " + totalRemaining);

        // Admin buttons
        if (isAdmin) {
            evh.tvEdit.setVisibility(View.VISIBLE);
            evh.tvDelete.setVisibility(View.VISIBLE);
        } else {
            evh.tvEdit.setVisibility(View.GONE);
            evh.tvDelete.setVisibility(View.GONE);
        }

        // --- REPLACED CLICK LISTENER ---
        evh.itemView.setOnClickListener(v -> {

            List<TicketFormData> ticketsList = e.getTickets();
            StringBuilder sb = new StringBuilder();
            int total = 0;

            if (ticketsList != null && !ticketsList.isEmpty()) {
                for (TicketFormData t : ticketsList) {
                    String label = (t.getName() != null && !t.getName().isEmpty())
                            ? t.getName()
                            : (t.getTypeName() != null ? t.getTypeName() : "Ticket");

                    sb.append(label)
                            .append(": ")
                            .append(t.getQuantity())
                            .append("\n");

                    total += t.getQuantity();
                }
            } else {
                sb.append("No tickets available\n");
            }

            sb.append("\nTotal remaining: ").append(total);

            // Inflate custom dialog
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_event_details, null);
            TextView tvContent = dialogView.findViewById(R.id.tvDialogContent);
            TextView tvOk = dialogView.findViewById(R.id.tvDialogOk);
            TextView tvBook = dialogView.findViewById(R.id.tvDialogBook);

            tvContent.setText(
                    "Event Details: " + e.getDetails() +
                            "\nEvent Date: " + e.getDate() +
                            "\n\nBooking Window" +
                            "\nSale Start: " + formatTime(e.getSaleStartTime()) +
                            "\nSale End: " + formatTime(e.getSaleEndTime()) + " or until Sold Out" +
                            "\n\nTickets:" +
                            "\n" + sb
            );

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .create();

            tvOk.setOnClickListener(view -> dialog.dismiss());

            tvBook.setOnClickListener(view -> {
                dialog.dismiss();

                // Start TicketBookingActivity and pass the selected Event
                Intent intent = new Intent(context, TicketBookingActivity.class);
                intent.putExtra("eventId", e.getEventId()); // make sure Event has getEventId()
                context.startActivity(intent);
            });


            dialog.show();
        });
    }


    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(millis);
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView tvEventName, tvEventDate, tvEventDetails, tvTickets;
        TextView tvSaleStart, tvSaleEnd;
        TextView tvEdit, tvDelete;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventDetails = itemView.findViewById(R.id.tvEventDetails);
            tvTickets = itemView.findViewById(R.id.tvTickets);
            tvSaleStart = itemView.findViewById(R.id.tvSaleStart);
            tvSaleEnd = itemView.findViewById(R.id.tvSaleEnd);
            tvEdit = itemView.findViewById(R.id.tvEdit);
            tvDelete = itemView.findViewById(R.id.tvDelete);
        }
    }
}
