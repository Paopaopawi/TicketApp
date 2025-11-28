package com.example.ticketapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.activities.AddEditEventActivity;
import com.example.ticketapp.activities.TicketBookingActivity;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.models.TicketFormData;
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class UserEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EVENT = 1;

    private final Context context;
    private final List<Object> itemList;
    private final boolean isAdmin;

    private final long BOOKING_COOLDOWN = 10 * 60 * 1000; // 10 minutes

    // ------------------- CLICK LISTENER -------------------
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private OnEventClickListener eventClickListener;

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.eventClickListener = listener;
    }

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
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_event_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_event_card, parent, false);
            return new EventViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) itemList.get(position));
        } else if (holder instanceof EventViewHolder) {
            ((EventViewHolder) holder).bind((Event) itemList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    // ------------------- ViewHolders -------------------

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }

        void bind(String header) {
            tvHeader.setText(header != null ? header : "");
        }
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvEventDate, tvEventDetails, tvTickets;
        TextView tvSaleStart, tvSaleEnd;
        TextView tvEdit, tvDelete;
        Button btnBookNow;

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
            btnBookNow = itemView.findViewById(R.id.btnBookNow);
        }

        void bind(Event e) {
            if (e == null) return;

            tvEventName.setText(e.getTitle() != null ? e.getTitle() : "-");
            tvEventDate.setText("Date: " + (e.getDate() != null ? e.getDate() : "-"));
            tvEventDetails.setText("Details: " + (e.getDetails() != null ? e.getDetails() : "-"));

            int totalAvailable = 0;
            int totalOnHold = 0;
            if (e.getTickets() != null) {
                for (Object obj : e.getTickets()) {
                    TicketFormData t = null;
                    if (obj instanceof TicketFormData) {
                        t = (TicketFormData) obj;
                    } else if (obj instanceof HashMap) {
                        HashMap map = (HashMap) obj;
                        t = new TicketFormData();
                        t.setType((String) map.get("typeName"));
                        t.setPrice(map.get("price") != null ? (int) Double.parseDouble(map.get("price").toString()) : 0);
                        t.setAvailableQuantity(map.get("availableQuantity") != null ? Integer.parseInt(map.get("availableQuantity").toString()) : 0);
                        t.setOnHoldQuantity(map.get("onHoldQuantity") != null ? Integer.parseInt(map.get("onHoldQuantity").toString()) : 0);
                    }
                    if (t != null) {
                        totalAvailable += Math.max(0, t.getAvailableQuantity() - t.getOnHoldQuantity());
                        totalOnHold += t.getOnHoldQuantity();
                    }
                }
            }

            tvTickets.setText("Tickets left: " + totalAvailable);
            tvSaleStart.setText("Sale Start: " + formatTime(e.getSaleStartTime()));
            tvSaleEnd.setText("Sale End: " + formatTime(e.getSaleEndTime()));

            tvEdit.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            tvDelete.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

            if (isAdmin) {
                tvEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(context, AddEditEventActivity.class);
                    intent.putExtra("eventId", e.getEventId());
                    context.startActivity(intent);
                });

                tvDelete.setOnClickListener(v -> {
                    FirebaseHelper.getEventsRef().child(e.getEventId())
                            .removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(err -> Toast.makeText(context, "Delete failed: " + err.getMessage(), Toast.LENGTH_LONG).show());
                });
            }

            if (isAdmin) {
                btnBookNow.setVisibility(View.GONE);
            } else {
                btnBookNow.setVisibility(View.VISIBLE);
                updateBookingButton(btnBookNow, e, totalAvailable);

                btnBookNow.setOnClickListener(v -> {
                    if (btnBookNow.isEnabled() && eventClickListener != null) {
                        eventClickListener.onEventClick(e);
                    }
                });
            }

            // ---------------- Popup Dialog ----------------
            int finalTotalAvailable = totalAvailable;
            int finalTotalOnHold = totalOnHold;

            itemView.setOnClickListener(v -> {
                if (eventClickListener != null) {
                    eventClickListener.onEventClick(e);
                }

                android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context).create();
                View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_event_details, null);
                TextView tvContent = dialogView.findViewById(R.id.tvDialogContent);
                TextView tvOk = dialogView.findViewById(R.id.tvDialogOk);
                TextView tvBook = dialogView.findViewById(R.id.tvDialogBook);

                StringBuilder content = new StringBuilder();
                content.append("Title: ").append(e.getTitle() != null ? e.getTitle() : "-").append("\n\n");
                content.append("Date: ").append(e.getDate() != null ? e.getDate() : "-").append("\n\n");
                content.append("Details: ").append(e.getDetails() != null ? e.getDetails() : "-").append("\n\n");
                content.append("Tickets:\n");
                content.append("Sale Start: ").append(formatTime(e.getSaleStartTime())).append("\n");
                content.append("Sale End: ").append(formatTime(e.getSaleEndTime())).append("\n");
                content.append("Available Tickets: ").append(finalTotalAvailable).append("\n");
                content.append("On Hold Tickets: ").append(finalTotalOnHold);

                tvContent.setText(content.toString());

                if (isAdmin) {
                    tvBook.setVisibility(View.GONE);
                } else {
                    tvBook.setVisibility(View.VISIBLE);
                    updateBookingButton(tvBook, e, finalTotalAvailable);
                    tvBook.setOnClickListener(x -> {
                        if (tvBook.isEnabled() && eventClickListener != null) {
                            eventClickListener.onEventClick(e);
                            dialog.dismiss();
                        }
                    });
                }

                tvOk.setOnClickListener(x -> dialog.dismiss());
                dialog.setView(dialogView);
                dialog.show();
            });
        }

        private void updateBookingButton(View btn, Event e, int totalAvailable) {
            long now = System.currentTimeMillis();
            if (totalAvailable <= 0 || now < e.getSaleStartTime() || now > e.getSaleEndTime()) {
                btn.setEnabled(false);
                if (totalAvailable <= 0) ((TextView) btn).setText("Sold Out");
                else ((TextView) btn).setText("Booking Closed");
                btn.setAlpha(0.5f);
            } else {
                btn.setEnabled(true);
                ((TextView) btn).setText("Book Now");
                btn.setAlpha(1f);
            }
        }

        private String formatTime(long millis) {
            if (millis <= 0) return "-";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(millis));
        }
    }
}
