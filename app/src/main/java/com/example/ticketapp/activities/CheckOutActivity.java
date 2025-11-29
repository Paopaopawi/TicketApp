package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckOutActivity extends AppCompatActivity {

    private RecyclerView rvHeldTickets;
    private TextView tvPaymentSubtotal, tvPaymentServiceFee, tvPaymentTotalDue, tvPaymentTimer;
    private Spinner spinnerPaymentMethod;
    private Button btnProceedPayment;

    private List<TicketFormData> heldTickets = new ArrayList<>();
    private CheckOutAdapter adapter;

    private static final double SERVICE_FEE_PERCENT = 0.025;
    private Map<String, CountDownTimer> timersByHold = new HashMap<>();
    private List<CountDownTimer> timers = new ArrayList<>();

    // NEW: totals as fields so they can be accessed in button click
    private int finalSubtotal;
    private int finalServiceFee;
    private int finalTotal;

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

        heldTickets = getIntent().getParcelableArrayListExtra("selectedTickets");
        if (heldTickets == null) heldTickets = new ArrayList<>();

        if (heldTickets.isEmpty()) {
            Toast.makeText(this, "No tickets to pay for", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String[] paymentMethods = {"GCash", "PayMaya", "Credit Card", "Debit Card"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                paymentMethods
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(spinnerAdapter);
        

        adapter = new CheckOutAdapter(heldTickets);
        rvHeldTickets.setLayoutManager(new LinearLayoutManager(this));
        rvHeldTickets.setAdapter(adapter);

        calculateTotals();
        startHoldCountdowns();

        btnProceedPayment.setOnClickListener(v -> {
            Log.d("CHECKOUT_DEBUG", "Proceed Payment clicked");

            if (isAnyHoldExpired()) {
                Log.d("CHECKOUT_DEBUG", "Payment blocked - expired hold detected");
                Toast.makeText(this, "Some tickets have expired. Please reselect tickets.", Toast.LENGTH_LONG).show();
                return;
            }

            // Payment succeeded
            handlePaymentSuccess();
        });
    }

    private boolean isAnyHoldExpired() {
        long now = System.currentTimeMillis();
        for (TicketFormData t : heldTickets) {
            Log.d("HOLD_VERIFY", "Verify ticket: " + t.getName() +
                    " holdExpiresAt=" + t.getPerHoldExpiry() + " now=" + now);
            if (t.getPerHoldExpiry() <= now) {
                Log.d("HOLD_VERIFY", "EXPIRED DETECTED for " + t.getName());
                return true;
            }
        }
        return false;
    }

    private void calculateTotals() {
        finalSubtotal = 0;
        for (TicketFormData t : heldTickets) {
            finalSubtotal += t.getOnHoldQuantity() * t.getPrice();
        }
        finalServiceFee = (int)(finalSubtotal * SERVICE_FEE_PERCENT);
        finalTotal = finalSubtotal + finalServiceFee;

        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        tvPaymentSubtotal.setText("Subtotal: ₱" + nf.format(finalSubtotal));
        tvPaymentServiceFee.setText("Service Fees (2.5%): ₱" + nf.format(finalServiceFee));
        tvPaymentTotalDue.setText("TOTAL DUE: ₱" + nf.format(finalTotal));

        Log.d("CHECKOUT_DEBUG", "Totals calculated: subtotal=" + finalSubtotal +
                " serviceFee=" + finalServiceFee + " total=" + finalTotal);
    }

    private void startHoldCountdowns() {
        Log.d("COUNTDOWN_DEBUG", "startHoldCountdowns() triggered");

        for (CountDownTimer t : timers) t.cancel();
        timers.clear();
        for (CountDownTimer t : timersByHold.values()) t.cancel();
        timersByHold.clear();

        long nearestExpiry = Long.MAX_VALUE;

        for (TicketFormData t : heldTickets) {
            long holdExpiry = t.getPerHoldExpiry();
            String holdId = t.getHoldId();
            if (holdExpiry <= 0 || holdId == null) continue;

            long remaining = holdExpiry - System.currentTimeMillis();
            if (remaining <= 0) {
                removeExpiredHold(t);
                continue;
            }

            nearestExpiry = Math.min(nearestExpiry, holdExpiry);

            CountDownTimer holdTimer = new CountDownTimer(remaining, 1000) {
                @Override
                public void onTick(long msLeft) {
                    long minutes = (msLeft / 1000) / 60;
                    long seconds = (msLeft / 1000) % 60;
                    tvPaymentTimer.setText(String.format("Expires in %02d:%02d", minutes, seconds));
                }

                @Override
                public void onFinish() {
                    t.setOnHoldQuantity(0);
                    adapter.notifyDataSetChanged();
                    removeExpiredHold(t);
                }
            }.start();

            timersByHold.put(holdId, holdTimer);
            timers.add(holdTimer);
        }

        if (nearestExpiry == Long.MAX_VALUE) {
            tvPaymentTimer.setText("Hold expired");
            btnProceedPayment.setEnabled(false);
        }
    }

    private void removeExpiredHold(TicketFormData t) {
        if (t.getHoldId() == null || t.getEventId() == null || t.getTicketKey() == null) return;

        DatabaseReference ref = FirebaseHelper.getEventsRef()
                .child(t.getEventId())
                .child("tickets")
                .child(t.getTicketKey());

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                TicketFormData ticketData = currentData.getValue(TicketFormData.class);
                if (ticketData == null) return Transaction.success(currentData);

                MutableData holdsNode = currentData.child("holds");
                if (holdsNode.getValue() != null && holdsNode.hasChild(t.getHoldId())) {
                    MutableData thisHold = holdsNode.child(t.getHoldId());
                    Integer qty = thisHold.child("quantity").getValue(Integer.class);
                    if (qty == null) qty = 0;

                    thisHold.setValue(null);

                    int totalTickets = ticketData.getAvailableQuantity() + ticketData.getOnHoldQuantity();
                    ticketData.setOnHoldQuantity(Math.max(ticketData.getOnHoldQuantity() - qty, 0));
                    ticketData.setAvailableQuantity(Math.max(totalTickets - ticketData.getOnHoldQuantity(), 0));

                    currentData.child("availableQuantity").setValue(ticketData.getAvailableQuantity());
                    currentData.child("onHoldQuantity").setValue(ticketData.getOnHoldQuantity());
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@NonNull DatabaseError error, boolean committed, DataSnapshot dataSnapshot) {
                CountDownTimer timer = timersByHold.get(t.getHoldId());
                if (timer != null) {
                    timer.cancel();
                    timersByHold.remove(t.getHoldId());
                }
            }
        });
    }

    // ---------------- Payment Success Logic ----------------
    private void handlePaymentSuccess() {
        Log.d("PAYMENT_DEBUG", "Payment succeeded");

        DatabaseReference bookingsRef = FirebaseHelper.getRootRef().child("bookings");
        String bookingId = bookingsRef.push().getKey();
        if (bookingId == null) return;

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", bookingId);
        bookingData.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        bookingData.put("status", "paid");
        bookingData.put("createdAt", System.currentTimeMillis());

        Map<String, Object> ticketsNode = new HashMap<>();

        // Assign code to each TicketFormData BEFORE sending to PaymentSuccess
        for (TicketFormData t : heldTickets) {
            String code = generate6DigitCode();
            t.setTicketCode(code); // assign code to object
            removeHoldAfterPayment(t); // remove hold from Firebase

            // Prepare map for Firebase
            Map<String, Object> ticketInfo = new HashMap<>();
            ticketInfo.put("ticketKey", t.getTicketKey());
            ticketInfo.put("quantity", t.getOnHoldQuantity());
            ticketInfo.put("price", t.getPrice());
            ticketInfo.put("code", code);
            ticketInfo.put("ticketName", t.getName());
            ticketsNode.put(t.getTicketKey(), ticketInfo);
        }

        bookingData.put("tickets", ticketsNode);

        bookingsRef.child(bookingId).setValue(bookingData)
                .addOnSuccessListener(aVoid -> {
                    // Pass tickets with code to PaymentSuccess
                    Intent it = new Intent(CheckOutActivity.this, PaymentSuccess.class);
                    it.putExtra("subtotal", finalSubtotal);
                    it.putExtra("serviceFee", finalServiceFee);
                    it.putExtra("total", finalTotal);
                    it.putParcelableArrayListExtra("tickets", new ArrayList<>(heldTickets));
                    startActivity(it);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save booking", Toast.LENGTH_SHORT).show();
                });
    }


    private void removeHoldAfterPayment(TicketFormData t) {
        if (t.getHoldId() == null || t.getEventId() == null || t.getTicketKey() == null) return;

        DatabaseReference ref = FirebaseHelper.getEventsRef()
                .child(t.getEventId())
                .child("tickets")
                .child(t.getTicketKey());

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                TicketFormData ticketData = currentData.getValue(TicketFormData.class);
                if (ticketData == null) return Transaction.success(currentData);

                MutableData holdsNode = currentData.child("holds");
                if (holdsNode.getValue() != null && holdsNode.hasChild(t.getHoldId())) {
                    MutableData thisHold = holdsNode.child(t.getHoldId());
                    Integer qty = thisHold.child("quantity").getValue(Integer.class);
                    if (qty == null) qty = 0;

                    // Remove the hold
                    thisHold.setValue(null);

                    // ✅ Decrement both onHold and available by qty
                    ticketData.setOnHoldQuantity(Math.max(ticketData.getOnHoldQuantity() - qty, 0));
                    ticketData.setAvailableQuantity(Math.max(ticketData.getAvailableQuantity() - qty, 0));

                    currentData.child("availableQuantity").setValue(ticketData.getAvailableQuantity());
                    currentData.child("onHoldQuantity").setValue(ticketData.getOnHoldQuantity());
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@NonNull DatabaseError error, boolean committed, DataSnapshot dataSnapshot) {
                CountDownTimer timer = timersByHold.get(t.getHoldId());
                if (timer != null) {
                    timer.cancel();
                    timersByHold.remove(t.getHoldId());
                }
            }
        });
    }

    private String generate6DigitCode() {
        int code = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}
