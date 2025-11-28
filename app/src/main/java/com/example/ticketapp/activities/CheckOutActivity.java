package com.example.ticketapp.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.CheckOutAdapter;
import com.example.ticketapp.models.TicketFormData;
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckOutActivity extends AppCompatActivity {

    private RecyclerView rvHeldTickets;
    private TextView tvPaymentSubtotal, tvPaymentServiceFee, tvPaymentTotalDue, tvPaymentTimer;
    private Spinner spinnerPaymentMethod;
    private Button btnProceedPayment;

    private List<TicketFormData> heldTickets = new ArrayList<>();
    private CheckOutAdapter adapter;

    private static final double SERVICE_FEE_PERCENT = 0.025;
    private List<CountDownTimer> timers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        rvHeldTickets = findViewById(R.id.rvHeldTickets);
        tvPaymentSubtotal = findViewById(R.id.tvPaymentSubtotal);
        tvPaymentServiceFee = findViewById(R.id.tvPaymentServiceFee);
        tvPaymentTotalDue = findViewById(R.id.tvPaymentTotalDue);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);
        btnProceedPayment = findViewById(R.id.btnProceedPayment);
        tvPaymentTimer = findViewById(R.id.tvPaymentTimer);

        //noinspection unchecked
        heldTickets = getIntent().getParcelableArrayListExtra("selectedTickets");
        if (heldTickets == null) heldTickets = new ArrayList<>();

        if (heldTickets.isEmpty()) {
            Toast.makeText(this, "No tickets to pay for", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new CheckOutAdapter(heldTickets);
        rvHeldTickets.setLayoutManager(new LinearLayoutManager(this));
        rvHeldTickets.setAdapter(adapter);

        calculateTotals();
        startHoldCountdowns();

        btnProceedPayment.setOnClickListener(v -> {
            if (isAnyHoldExpired()) {
                Toast.makeText(this, "Some tickets have expired. Please reselect tickets.", Toast.LENGTH_LONG).show();
                return;
            }
            // integrate real payment here
            Toast.makeText(this, "Payment flow not implemented", Toast.LENGTH_SHORT).show();
        });
    }

    private void calculateTotals() {
        int subtotal = 0;
        for (TicketFormData t : heldTickets) {
            subtotal += t.getOnHoldQuantity() * t.getPrice();
        }
        double serviceFee = subtotal * SERVICE_FEE_PERCENT;
        double total = subtotal + serviceFee;

        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        tvPaymentSubtotal.setText("Subtotal: ₱" + nf.format(subtotal));
        tvPaymentServiceFee.setText("Service Fees (2.5%): ₱" + nf.format(serviceFee));
        tvPaymentTotalDue.setText("TOTAL DUE: ₱" + nf.format(total));
    }

    private void startHoldCountdowns() {
        // Cancel previous timers if any
        for (CountDownTimer t : timers) t.cancel();
        timers.clear();

        long nearestExpiry = Long.MAX_VALUE;
        for (TicketFormData t : heldTickets) {
            long holdExpiry = t.getExpiresAt(); // 🔥 use holdExpiresAt from hold node
            if (holdExpiry > System.currentTimeMillis()) nearestExpiry = Math.min(nearestExpiry, holdExpiry);
        }

        if (nearestExpiry == Long.MAX_VALUE) {
            tvPaymentTimer.setText("Hold expired");
            btnProceedPayment.setEnabled(false);
            return;
        }

        long countdownTime = nearestExpiry - System.currentTimeMillis();
        if (countdownTime <= 0) {
            tvPaymentTimer.setText("Hold expired");
            btnProceedPayment.setEnabled(false);
            return;
        }

        CountDownTimer timer = new CountDownTimer(countdownTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvPaymentTimer.setText(String.format("Expires in %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvPaymentTimer.setText("Hold expired");
                for (TicketFormData t : heldTickets) t.setOnHoldQuantity(0);
                removeAllExpiredHolds();
                adapter.notifyDataSetChanged();
                btnProceedPayment.setEnabled(false);
            }
        }.start();

        timers.add(timer);
    }

    private boolean isAnyHoldExpired() {
        long now = System.currentTimeMillis();
        for (TicketFormData t : heldTickets) {
            if (t.getExpiresAt() <= now) return true; // 🔥 use holdExpiresAt
        }
        return false;
    }

    private void removeAllExpiredHolds() {
        for (TicketFormData t : heldTickets) {
            removeExpiredHold(t);
        }
    }

    private void removeExpiredHold(TicketFormData t) {
        if (t.getHoldId() == null || t.getEventId() == null || t.getTicketKey() == null) return;

        DatabaseReference eventTicketRef = FirebaseHelper.getEventsRef()
                .child(t.getEventId())
                .child("tickets")
                .child(t.getTicketKey());

        eventTicketRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                TicketFormData ticketData = currentData.getValue(TicketFormData.class);
                if (ticketData == null) return Transaction.success(currentData);

                MutableData holdsNode = currentData.child("holds");
                if (holdsNode.getValue() != null) {
                    List<String> expiredKeys = new ArrayList<>();
                    int activeOnHold = 0;

                    for (MutableData holdSnap : holdsNode.getChildren()) {
                        Long expiresAt = holdSnap.child("expiresAt").getValue(Long.class);
                        Integer qty = holdSnap.child("quantity").getValue(Integer.class);
                        if (qty == null) qty = 0;

                        if (holdSnap.getKey().equals(t.getHoldId()) || (expiresAt != null && expiresAt <= System.currentTimeMillis())) {
                            expiredKeys.add(holdSnap.getKey());
                        } else {
                            activeOnHold += qty;
                        }
                    }

                    for (String holdKey : expiredKeys) holdsNode.child(holdKey).setValue(null);

                    int totalTickets = ticketData.getAvailableQuantity() + ticketData.getOnHoldQuantity();
                    ticketData.setOnHoldQuantity(activeOnHold);
                    ticketData.setAvailableQuantity(Math.max(totalTickets - activeOnHold, 0));

                    currentData.setValue(ticketData);
                    Log.d("CHECKOUT_DEBUG", "Updated ticket quantities for " + ticketData.getName() +
                            ": available=" + ticketData.getAvailableQuantity() +
                            ", onHold=" + ticketData.getOnHoldQuantity());
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@NonNull DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (committed) {
                    Log.d("CHECKOUT_DEBUG", "Hold " + t.getHoldId() + " removed successfully.");
                }
            }
        });
    }
}
