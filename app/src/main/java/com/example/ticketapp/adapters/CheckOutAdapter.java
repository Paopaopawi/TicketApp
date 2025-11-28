package com.example.ticketapp.adapters;

import android.os.CountDownTimer;
import android.util.Log;
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
        Log.d("CHECKOUT_DEBUG",
                "---- DEBUG HOLD INFO ----\n" +
                        "Ticket: " + t.getName() + "\n" +
                        "expiresAt (raw): " + t.getExpiresAt() + "\n" +
                        "System.currentTimeMillis(): " + System.currentTimeMillis() + "\n" +
                        "Difference (timeLeft): " + (t.getExpiresAt() - System.currentTimeMillis()) + "\n" +
                        "expiresAt converted: " +
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                .format(new java.util.Date(t.getExpiresAt())) + "\n" +
                        "Now converted: " +
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                .format(new java.util.Date(System.currentTimeMillis()))
        );
        holder.tvName.setText(t.getName());
        holder.tvType.setText("Type: " + t.getType());
        holder.tvQuantity.setText("Quantity: " + t.getOnHoldQuantity());
        holder.tvPrice.setText("Price/ticket: ₱" + NumberFormat.getInstance(Locale.getDefault()).format(t.getPrice()));
        int subtotal = t.getOnHoldQuantity() * t.getPrice();
        holder.tvSubtotal.setText("Subtotal: ₱" + NumberFormat.getInstance(Locale.getDefault()).format(subtotal));

        // Countdown logic
        long timeLeft = t.getExpiresAt() - System.currentTimeMillis();
        if (timeLeft > 0) {
            new CountDownTimer(timeLeft, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long minutes = (millisUntilFinished / 1000) / 60;
                    long seconds = (millisUntilFinished / 1000) % 60;
                    holder.tvTimer.setText(String.format("Expires in %02d:%02d", minutes, seconds));
                }

                @Override
                public void onFinish() {
                    holder.tvTimer.setText("Hold expired");
                    // Note: Firebase cleanup handled in Activity
                }
            }.start();
        } else {
            holder.tvTimer.setText("Hold expired");
        }

    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvQuantity, tvPrice, tvSubtotal, tvTimer;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPaymentTicketName);
            tvType = itemView.findViewById(R.id.tvPaymentTicketType);
            tvQuantity = itemView.findViewById(R.id.tvPaymentTicketQuantity);
            tvPrice = itemView.findViewById(R.id.tvPaymentTicketPrice);
            tvSubtotal = itemView.findViewById(R.id.tvPaymentTicketSubtotal);
            tvTimer = itemView.findViewById(R.id.tvPaymentTimer);
        }
    }
}
