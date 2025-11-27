package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.TicketSelectionAdapter;
import com.example.ticketapp.models.TicketFormData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TicketBookingActivity extends AppCompatActivity {

    private TextView tvBookingTitle, tvTimer, tvTotalSelected, tvSubtotal, tvServiceFee, tvTotalDue;
    private RecyclerView rvTickets;
    private Button btnConfirmBooking;

    private List<TicketFormData> ticketsList;
    private Map<String, Integer> selectedQuantities;
    private Map<String, String> ticketNameToKey; // Store Firebase keys
    private TicketSelectionAdapter adapter;

    private static final int MAX_PER_USER = 4;
    private static final double SERVICE_FEE_PERCENT = 0.025;

    private CountDownTimer countDownTimer;
    private long remainingMillis = 5 * 60 * 1000; // 5 min
    private String eventId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_booking);

        tvBookingTitle = findViewById(R.id.tvBookingTitle);
        tvTimer = findViewById(R.id.tvTimer);
        tvTotalSelected = findViewById(R.id.tvTotalSelected);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvTotalDue = findViewById(R.id.tvTotalDue);
        rvTickets = findViewById(R.id.rvTickets);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        ticketsList = new ArrayList<>();
        selectedQuantities = new HashMap<>();
        ticketNameToKey = new HashMap<>();

        eventId = getIntent().getStringExtra("eventId");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTicketsFromFirebase();
        startTimer();

        btnConfirmBooking.setOnClickListener(v -> holdSelectedTickets());
    }

    private void loadTicketsFromFirebase() {
        FirebaseDatabase.getInstance().getReference("events")
                .child(eventId)
                .child("tickets")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ticketsList.clear();
                        selectedQuantities.clear();
                        ticketNameToKey.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            TicketFormData t = ds.getValue(TicketFormData.class);
                            if (t != null) {
                                ticketsList.add(t);
                                selectedQuantities.put(t.getName(), 0);
                                ticketNameToKey.put(t.getName(), ds.getKey()); // store actual Firebase key
                            }
                        }

                        adapter = new TicketSelectionAdapter(
                                ticketsList,
                                selectedQuantities,
                                TicketBookingActivity.this::updateSummary
                        );
                        rvTickets.setLayoutManager(new LinearLayoutManager(TicketBookingActivity.this));
                        rvTickets.setAdapter(adapter);
                        updateSummary();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TicketBookingActivity.this, "Failed to load tickets", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void holdSelectedTickets() {
        int totalSelected = getTotalSelectedTickets();
        if (totalSelected == 0) {
            Toast.makeText(this, "Select at least one ticket", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ticketsRef = FirebaseDatabase.getInstance()
                .getReference("events")
                .child(eventId)
                .child("tickets");

        Map<String, Integer> quantitiesToHold = new HashMap<>();
        for (TicketFormData t : ticketsList) {
            int qty = selectedQuantities.getOrDefault(t.getName(), 0);
            if (qty > 0) quantitiesToHold.put(t.getName(), qty);
        }

        long expiresAt = System.currentTimeMillis() + remainingMillis;
        List<TicketFormData> heldTickets = new ArrayList<>();
        final int[] completedTransactions = {0};
        final int totalTicketsToHold = quantitiesToHold.size();

        for (Map.Entry<String, Integer> entry : quantitiesToHold.entrySet()) {
            String ticketName = entry.getKey();
            int qtyToHold = entry.getValue();

            String firebaseKey = ticketNameToKey.get(ticketName); // get correct Firebase key
            if (firebaseKey == null) {
                Toast.makeText(this, "Ticket key not found for " + ticketName, Toast.LENGTH_SHORT).show();
                continue;
            }

            ticketsRef.child(firebaseKey).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    TicketFormData t = currentData.getValue(TicketFormData.class);
                    if (t == null) return Transaction.abort();

                    Log.d("TicketBooking", "Holding ticket: " + ticketName +
                            ", available: " + t.getQuantity() +
                            ", qtyToHold: " + qtyToHold);

                    if (t.getQuantity() >= qtyToHold) {
                        t.setQuantity(t.getQuantity() - qtyToHold);
                        currentData.setValue(t);
                        return Transaction.success(currentData);
                    } else {
                        return Transaction.abort();
                    }
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                    completedTransactions[0]++;
                    if (committed) {
                        FirebaseDatabase.getInstance().getReference("holds")
                                .child(userId)
                                .child(eventId)
                                .child(ticketName)
                                .setValue(new HashMap<String, Object>() {{
                                    put("quantity", qtyToHold);
                                    put("expiresAt", expiresAt);
                                }});

                        TicketFormData heldTicket = currentData.getValue(TicketFormData.class);
                        if (heldTicket != null) {
                            heldTicket.setQuantity(qtyToHold);
                            heldTickets.add(heldTicket);
                        }
                    } else {
                        Log.d("TicketBooking", "Failed to hold " + ticketName + ": " + (error != null ? error.getMessage() : "unknown"));
                        Toast.makeText(TicketBookingActivity.this,
                                "Failed to hold " + ticketName, Toast.LENGTH_SHORT).show();
                    }

                    if (completedTransactions[0] == totalTicketsToHold) {
                        if (!heldTickets.isEmpty()) {
                            Toast.makeText(TicketBookingActivity.this, "Tickets held! Proceeding to payment.", Toast.LENGTH_SHORT).show();
                            goToPayment(heldTickets);
                            startHoldCountdown(expiresAt);
                        }
                    }
                }
            });
        }
    }

    private void goToPayment(List<TicketFormData> heldTickets) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putParcelableArrayListExtra("selectedTickets", new ArrayList<>(heldTickets));
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }

    private void startHoldCountdown(long expiresAtMillis) {
        long millisLeft = expiresAtMillis - System.currentTimeMillis();
        new CountDownTimer(millisLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(TicketBookingActivity.this, "Hold expired! Tickets released.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.start();
    }

    private int getTotalSelectedTickets() {
        int total = 0;
        for (int qty : selectedQuantities.values()) total += qty;
        return total;
    }

    private double getSubtotal() {
        double subtotal = 0;
        for (TicketFormData t : ticketsList) {
            subtotal += t.getPrice() * selectedQuantities.getOrDefault(t.getName(), 0);
        }
        return subtotal;
    }

    private void updateSummary() {
        int totalSelected = getTotalSelectedTickets();
        double subtotal = getSubtotal();
        double serviceFee = subtotal * SERVICE_FEE_PERCENT;
        double totalDue = subtotal + serviceFee;

        tvTotalSelected.setText("Total Tickets Selected: " + totalSelected + " (Max " + MAX_PER_USER + ")");
        tvSubtotal.setText("Subtotal: ₱" + NumberFormat.getInstance(Locale.getDefault()).format((int)subtotal));
        tvServiceFee.setText("Service Fees (2.5%): ₱" + NumberFormat.getInstance(Locale.getDefault()).format((int)serviceFee));
        tvTotalDue.setText("TOTAL DUE: ₱" + NumberFormat.getInstance(Locale.getDefault()).format((int)totalDue));
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(TicketBookingActivity.this, "Time expired! Tickets released.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
