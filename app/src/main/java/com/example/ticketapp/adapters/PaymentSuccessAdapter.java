package com.example.ticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.TicketFormData;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PaymentSuccessAdapter extends RecyclerView.Adapter<PaymentSuccessAdapter.TicketViewHolder> {

    private final List<TicketFormData> tickets;

    public PaymentSuccessAdapter(List<TicketFormData> tickets) {
        this.tickets = tickets;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_ticket_card, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        TicketFormData ticket = tickets.get(position);

        holder.tvName.setText(ticket.getName() != null ? ticket.getName() : "Unknown Ticket");
        holder.tvQuantity.setText("Quantity: " + ticket.getOnHoldQuantity());
        holder.tvPrice.setText("Price: ₱" + NumberFormat.getInstance(Locale.getDefault()).format(ticket.getPrice()));
        int subtotal = ticket.getPrice() * ticket.getOnHoldQuantity();
        holder.tvSubtotal.setText("Subtotal: ₱" + NumberFormat.getInstance(Locale.getDefault()).format(subtotal));
        holder.tvCode.setText("Code: " + (ticket.getTicketCode() != null ? ticket.getTicketCode() : "N/A"));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvQuantity, tvPrice, tvSubtotal, tvCode;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserTicketName);
            tvQuantity = itemView.findViewById(R.id.tvUserTicketQuantity);
            tvPrice = itemView.findViewById(R.id.tvUserTicketPrice);
            tvSubtotal = itemView.findViewById(R.id.tvUserTicketSubtotal);
            tvCode = itemView.findViewById(R.id.tvUserTicketCode);
        }
    }
}
