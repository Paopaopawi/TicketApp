package com.example.ticketapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.models.TicketFormData;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Object> items;
    private final Context context;

    private final OnEventClickListener onEventClickListener;
    private final OnEditClickListener onEditClickListener;
    private final OnDeleteClickListener onDeleteClickListener;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_EVENT = 1;

    public interface OnEventClickListener { void onClick(Event e); }
    public interface OnEditClickListener { void onClick(Event e); }
    public interface OnDeleteClickListener { void onClick(Event e); }

    public EventAdapter(Context context, List<Object> items,
                        OnEventClickListener onEventClickListener,
                        OnEditClickListener onEditClickListener,
                        OnDeleteClickListener onDeleteClickListener) {
        this.context = context;
        this.items = items;
        this.onEventClickListener = onEventClickListener;
        this.onEditClickListener = onEditClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? VIEW_TYPE_HEADER : VIEW_TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_HEADER){
            View view = LayoutInflater.from(context).inflate(R.layout.item_event_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
            return new EventViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object obj = items.get(position);

        if(holder instanceof HeaderViewHolder){
            ((HeaderViewHolder) holder).tvHeader.setText((String) obj);

        } else if(holder instanceof EventViewHolder){
            Event e = (Event) obj;
            EventViewHolder evh = (EventViewHolder) holder;

            evh.tvEventName.setText(e.getTitle());
            evh.tvEventDate.setText("Date: " + e.getDate());
            evh.tvEventDetails.setText("Details: " + e.getDetails());
            evh.tvTickets.setText("Tickets: " + e.getTotalRemainingTickets() + " Available");

            evh.tvSaleStart.setText("Sale Start: " + formatMillis(e.getSaleStartTime()));
            evh.tvSaleEnd.setText("Sale End: " + formatMillis(e.getSaleEndTime()));

            // Click listeners for edit/delete
            evh.tvEdit.setOnClickListener(v -> onEditClickListener.onClick(e));
            evh.tvDelete.setOnClickListener(v -> onDeleteClickListener.onClick(e));

            // --- POPUP DETAIL ON CARD CLICK ---
            evh.itemView.setOnClickListener(v -> {
                List<TicketFormData> tickets = e.getTickets();

                StringBuilder sb = new StringBuilder();
                int totalRemaining = 0;

                if (tickets != null && !tickets.isEmpty()) {
                    for (TicketFormData t : tickets) {
                        String label = (t.getName() != null && !t.getName().isEmpty())
                                ? t.getName()
                                : (t.getTypeName() != null ? t.getTypeName() : "Ticket");

                        sb.append(label)
                                .append(": ")
                                .append(t.getQuantity())
                                .append("\n");

                        totalRemaining += t.getQuantity();
                    }
                } else {
                    sb.append("No tickets available\n");
                }

                sb.append("\nTotal remaining: ").append(totalRemaining);

                // Inflate custom dialog
                View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_event_details, null);
                TextView tvContent = dialogView.findViewById(R.id.tvDialogContent);
                TextView tvOk = dialogView.findViewById(R.id.tvDialogOk);

                // Set content text in requested format
                tvContent.setText(
                        "Event Details: " + e.getDetails() +
                                "\nEvent Date: " + e.getDate() +
                                "\n\nBooking Window" +
                                "\nSale Start: " + formatMillis(e.getSaleStartTime()) +
                                "\nSale End: " + formatMillis(e.getSaleEndTime()) + " or until Sold Out" +
                                "\n\nTickets:" +
                                "\n" + sb
                );

                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .create();

                tvOk.setOnClickListener(view -> dialog.dismiss());
                dialog.show();
            });


            // Optional: pass the click event for outside handling if needed
            evh.itemView.setOnLongClickListener(v -> {
                onEventClickListener.onClick(e);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatMillis(long millis){
        if (millis <= 0) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(millis);
    }

    private String getSaleStatus(Event e){
        long now = System.currentTimeMillis();

        if(now < e.getSaleStartTime()) return "pending";
        else if(now >= e.getSaleStartTime() && now <= e.getSaleEndTime() && e.getTotalRemainingTickets() > 0)
            return "active";
        else return "ended";
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(@NonNull View itemView){
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView tvEventName, tvEventDate, tvEventDetails, tvTickets, tvSaleStart, tvSaleEnd, tvEdit, tvDelete;

        EventViewHolder(@NonNull View itemView){
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
