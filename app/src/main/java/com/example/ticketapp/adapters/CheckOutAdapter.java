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

public class CheckOutAdapter extends RecyclerView.Adapter<CheckOutAdapter.ViewHolder> {

    private final List<TicketFormData> items;

    public CheckOutAdapter(List<TicketFormData> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CheckOutAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_ticket, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckOutAdapter.ViewHolder holder, int position) {
        TicketFormData t = items.get(position);
        holder.tvName.setText(t.getName());
        holder.tvType.setText("Type: " + t.getType());
        holder.tvQuantity.setText("Quantity: " + t.getOnHoldQuantity());
        holder.tvPrice.setText("Price/ticket: ₱" + NumberFormat.getInstance(Locale.getDefault()).format(t.getPrice()));
        int subtotal = t.getOnHoldQuantity() * t.getPrice();
        holder.tvSubtotal.setText("Subtotal: ₱" + NumberFormat.getInstance(Locale.getDefault()).format(subtotal));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvQuantity, tvPrice, tvSubtotal;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPaymentTicketName);
            tvType = itemView.findViewById(R.id.tvPaymentTicketType);
            tvQuantity = itemView.findViewById(R.id.tvPaymentTicketQuantity);
            tvPrice = itemView.findViewById(R.id.tvPaymentTicketPrice);
            tvSubtotal = itemView.findViewById(R.id.tvPaymentTicketSubtotal);
        }
    }
}
