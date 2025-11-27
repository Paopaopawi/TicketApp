package com.example.ticketapp.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketapp.R;
import com.example.ticketapp.models.TicketFormData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvPaymentTimer, tvPaymentSubtotal, tvPaymentServiceFee, tvPaymentTotalDue;
    private Spinner spinnerPaymentMethod;
    private Button btnProceedPayment;

    private List<TicketFormData> heldTickets;
    private double subtotal, serviceFee, total;
    private static final double SERVICE_FEE_PERCENT = 0.025;

    private String userId;
    private String eventId;
    private CountDownTimer paymentTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // --- Initialize Views ---
        tvPaymentTimer = findViewById(R.id.tvPaymentTimer);
        tvPaymentSubtotal = findViewById(R.id.tvPaymentSubtotal);
        tvPaymentServiceFee = findViewById(R.id.tvPaymentServiceFee);
        tvPaymentTotalDue = findViewById(R.id.tvPaymentTotalDue);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);
        btnProceedPayment = findViewById(R.id.btnProceedPayment);

        // Firebase info
        eventId = getIntent().getStringExtra("eventId");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        heldTickets = new ArrayList<>();

        loadHeldTicketsFromFirebase();
    }

    private void loadHeldTicketsFromFirebase() {
        FirebaseDatabase.getInstance().getReference("holds")
                .child(userId)
                .child(eventId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        heldTickets.clear();

                        if (!snapshot.exists()) {
                            Toast.makeText(PaymentActivity.this, "No held tickets found!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        long earliestExpiry = Long.MAX_VALUE;

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String ticketName = ds.getKey();
                            int qty = ds.child("quantity").getValue(Integer.class) != null ? ds.child("quantity").getValue(Integer.class) : 0;
                            long expiresAt = ds.child("expiresAt").getValue(Long.class) != null ? ds.child("expiresAt").getValue(Long.class) : System.currentTimeMillis();

                            // Keep track of earliest expiry for countdown
                            if (expiresAt < earliestExpiry) earliestExpiry = expiresAt;

                            // Load ticket info from events/tickets
                            FirebaseDatabase.getInstance().getReference("events")
                                    .child(eventId)
                                    .child("tickets")
                                    .child(ticketName)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot ticketSnapshot) {
                                            if (ticketSnapshot.exists()) {
                                                String name = ticketSnapshot.child("name").getValue(String.class);
                                                String type = ticketSnapshot.child("type").getValue(String.class);
                                                int price = ticketSnapshot.child("price").getValue(Integer.class) != null ? ticketSnapshot.child("price").getValue(Integer.class) : 0;

                                                heldTickets.add(new TicketFormData(name, type, price, qty));
                                                calculateTotals();
                                                displayOrderSummary();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(PaymentActivity.this, "Failed to load ticket info", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        // Start countdown after all holds are read
                        startPaymentCountdown(earliestExpiry);

                        // Payment button click
                        btnProceedPayment.setOnClickListener(v -> proceedPayment());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PaymentActivity.this, "Failed to load held tickets", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateTotals() {
        subtotal = 0;
        for (TicketFormData t : heldTickets) {
            subtotal += t.getPrice() * t.getQuantity();
        }
        serviceFee = subtotal * SERVICE_FEE_PERCENT;
        total = subtotal + serviceFee;
    }

    private void displayOrderSummary() {
        tvPaymentSubtotal.setText("Subtotal: ₱" + NumberFormat.getInstance(Locale.getDefault()).format((int)subtotal));
        tvPaymentServiceFee.setText("Service Fees (2.5%): ₱" + NumberFormat.getInstance(Locale.getDefault()).format((int)serviceFee));
        tvPaymentTotalDue.setText("TOTAL DUE: ₱" + NumberFormat.getInstance(Locale.getDefault()).format((int)total));
    }

    private void startPaymentCountdown(long expiresAtMillis) {
        long millisLeft = expiresAtMillis - System.currentTimeMillis();
        if (millisLeft <= 0) {
            Toast.makeText(this, "Hold already expired!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        paymentTimer = new CountDownTimer(millisLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvPaymentTimer.setText(String.format("Time left to pay: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvPaymentTimer.setText("Time left to pay: 00:00");
                Toast.makeText(PaymentActivity.this, "Hold expired! Tickets released.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.start();
    }

    private void proceedPayment() {
        if (heldTickets.isEmpty()) {
            Toast.makeText(this, "No tickets to pay for", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("holds")
                .child(userId)
                .child(eventId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PaymentActivity.this, "Payment successful! Tickets purchased.", Toast.LENGTH_SHORT).show();
                    if (paymentTimer != null) paymentTimer.cancel();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(PaymentActivity.this, "Payment failed!", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (paymentTimer != null) paymentTimer.cancel();
    }
}
