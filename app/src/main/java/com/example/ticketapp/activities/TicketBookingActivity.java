package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TicketBookingActivity extends AppCompatActivity {

    private RecyclerView rvTickets;
    private TextView tvSubtotal, tvServiceFee, tvTotalDue;
    private Button btnConfirmBooking;

    private String eventId;
    private List<TicketFormData> ticketsList = new ArrayList<>();
    private Map<String, Integer> selectedQuantities = new HashMap<>();
    private Map<String, String> ticketNameToKey = new HashMap<>(); // name -> firebase child key
    private TicketSelectionAdapter adapter;

    private static final double SERVICE_FEE_PERCENT = 0.025;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_booking);

        rvTickets = findViewById(R.id.rvTickets);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvTotalDue = findViewById(R.id.tvTotalDue);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        rvTickets.setLayoutManager(new LinearLayoutManager(this));

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "No event specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTickets();

        btnConfirmBooking.setOnClickListener(v -> {
            // Run cleanupExpiredHolds first
            cleanupExpiredHolds(() -> holdAndProceed());
        });
    }

    private void loadTickets() {
        DatabaseReference ticketsRef = FirebaseHelper.getEventsRef().child(eventId).child("tickets");
        ticketsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ticketsList.clear();
                ticketNameToKey.clear();
                selectedQuantities.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    TicketFormData t = ds.getValue(TicketFormData.class);
                    if (t != null) {
                        ticketsList.add(t);
                        ticketNameToKey.put(t.getName(), ds.getKey());
                        selectedQuantities.put(t.getName(), 0);
                    }
                }
                adapter = new TicketSelectionAdapter(ticketsList, selectedQuantities, TicketBookingActivity.this::updateTotals);
                rvTickets.setAdapter(adapter);
                updateTotals();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TicketBookingActivity.this, "Failed to load tickets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotals() {
        int subtotal = 0;
        for (TicketFormData t : ticketsList) {
            int qty = selectedQuantities.getOrDefault(t.getName(), 0);
            subtotal += qty * t.getPrice();
        }
        int serviceFee = (int) Math.round(subtotal * SERVICE_FEE_PERCENT);
        int total = subtotal + serviceFee;

        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        tvSubtotal.setText("Subtotal: ₱" + nf.format(subtotal));
        tvServiceFee.setText("Service Fees (2.5%): ₱" + nf.format(serviceFee));
        tvTotalDue.setText("TOTAL DUE: ₱" + nf.format(total));
    }

    private void holdAndProceed() {
        // gather choices
        Map<String, Integer> toHold = new HashMap<>();
        int subtotal = 0;
        for (TicketFormData t : ticketsList) {
            int qty = selectedQuantities.getOrDefault(t.getName(), 0);
            if (qty > 0) {
                toHold.put(t.getName(), qty);
                subtotal += qty * t.getPrice();
            }
        }

        if (toHold.isEmpty()) {
            Toast.makeText(this, "Please select at least one ticket", Toast.LENGTH_SHORT).show();
            return;
        }

        final int serviceFee = (int) Math.round(subtotal * SERVICE_FEE_PERCENT);
        final int totalDue = subtotal + serviceFee;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ticketsRef = FirebaseHelper.getEventsRef().child(eventId).child("tickets");

        int finalSubtotal = subtotal;
        ticketsRef.get().addOnSuccessListener(snapshot -> {
            long now = System.currentTimeMillis();
            boolean hasActiveHold = false;
            int totalUserTickets = 0;

            for (DataSnapshot ticketSnap : snapshot.getChildren()) {
                DataSnapshot holdsSnap = ticketSnap.child("holds");
                for (DataSnapshot holdSnap : holdsSnap.getChildren()) {
                    String holdUserId = holdSnap.child("userId").getValue(String.class);
                    Long expiresAt = holdSnap.child("expiresAt").getValue(Long.class);

                    // 5-minute active hold check
                    if (uid.equals(holdUserId) && expiresAt != null && expiresAt > now) {
                        hasActiveHold = true;
                    }

                    // count total tickets for this user
                    if (uid.equals(holdUserId)) {
                        Integer qty = holdSnap.child("quantity").getValue(Integer.class);
                        if (qty != null) totalUserTickets += qty;
                    }
                }
            }

            // Check maximum 4 tickets
            int requestedQty = toHold.values().stream().mapToInt(Integer::intValue).sum();
            if (totalUserTickets + requestedQty > 4) {
                Toast.makeText(TicketBookingActivity.this,
                        "Cannot book more than 4 tickets in total for this event",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (hasActiveHold) {
                Toast.makeText(TicketBookingActivity.this,
                        "You can only book once every 5 minutes. Please wait for your current hold to expire.",
                        Toast.LENGTH_LONG).show();
            } else {
                createHolds(toHold, finalSubtotal);
            }

        }).addOnFailureListener(e -> Toast.makeText(TicketBookingActivity.this,
                "Failed to check active holds: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createHolds(Map<String, Integer> toHold, int subtotal) {
        final int serviceFee = (int) Math.round(subtotal * SERVICE_FEE_PERCENT);
        final int totalDue = subtotal + serviceFee;

        DatabaseReference offsetRef = FirebaseHelper.getEventsRef().getRoot().child(".info/serverTimeOffset");
        int finalSubtotal = subtotal;
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                long offset = snapshot.getValue(Long.class) != null ? snapshot.getValue(Long.class) : 0L;
                long estimatedServerTime = System.currentTimeMillis() + offset;
                long expiresAt = estimatedServerTime + (5 * 60 * 1000L); // 5 minutes

                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ticketsRef = FirebaseHelper.getEventsRef().child(eventId).child("tickets");

                final List<TicketFormData> heldTicketsForIntent = new ArrayList<>();
                final int[] processed = {0};
                final int totalToProcess = toHold.size();

                for (Map.Entry<String, Integer> entry : toHold.entrySet()) {
                    String ticketName = entry.getKey();
                    int qty = entry.getValue();
                    String key = ticketNameToKey.get(ticketName);
                    if (key == null) {
                        processed[0]++;
                        continue;
                    }

                    ticketsRef.child(key).runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            TicketFormData t = currentData.getValue(TicketFormData.class);
                            if (t == null) return Transaction.abort();

                            int freeQty = t.getAvailableQuantity() - t.getOnHoldQuantity();
                            if (freeQty >= qty) {
                                t.setOnHoldQuantity(t.getOnHoldQuantity() + qty);
                                currentData.setValue(t);
                                return Transaction.success(currentData);
                            }
                            return Transaction.abort();
                        }

                        @Override
                        public void onComplete(@NonNull DatabaseError error, boolean committed, DataSnapshot currentData) {
                            processed[0]++;
                            if (committed && currentData.exists()) {
                                String holdId = UUID.randomUUID().toString();
                                Map<String, Object> holdData = new HashMap<>();
                                holdData.put("userId", uid);
                                holdData.put("quantity", qty);
                                holdData.put("expiresAt", expiresAt);
                                holdData.put("subtotal", finalSubtotal);
                                holdData.put("serviceFee", serviceFee);
                                holdData.put("totalDue", totalDue);

                                ticketsRef.child(key).child("holds").child(holdId).setValue(holdData);

                                TicketFormData t = currentData.getValue(TicketFormData.class);
                                if (t != null) {
                                    TicketFormData copy = new TicketFormData(t.getName(), t.getType(), t.getPrice(), t.getAvailableQuantity());
                                    copy.setOnHoldQuantity(qty);
                                    heldTicketsForIntent.add(copy);
                                }
                            } else {
                                Toast.makeText(TicketBookingActivity.this, "Failed to hold " + ticketName + " (not enough available)", Toast.LENGTH_SHORT).show();
                            }

                            if (processed[0] == totalToProcess) {
                                if (!heldTicketsForIntent.isEmpty()) {
                                    Intent it = new Intent(TicketBookingActivity.this, CheckOutActivity.class);
                                    it.putParcelableArrayListExtra("selectedTickets", new ArrayList<>(heldTicketsForIntent));
                                    it.putExtra("eventId", eventId);
                                    it.putExtra("expiresAt", expiresAt);
                                    startActivity(it);
                                }
                            }
                        }
                    });
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TicketBookingActivity.this, "Failed to get server time offset: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------- NEW: Cleanup expired holds -------------------
    private void cleanupExpiredHolds(Runnable onComplete) {
        DatabaseReference ticketsRef = FirebaseHelper.getEventsRef().child(eventId).child("tickets");
        long now = System.currentTimeMillis();

        ticketsRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot ticketSnap : snapshot.getChildren()) {
                String key = ticketSnap.getKey();
                if (ticketSnap.child("holds").exists()) {
                    for (DataSnapshot holdSnap : ticketSnap.child("holds").getChildren()) {
                        Long expiresAt = holdSnap.child("expiresAt").getValue(Long.class);
                        Integer qty = holdSnap.child("quantity").getValue(Integer.class);
                        if (expiresAt != null && expiresAt <= now && qty != null) {
                            // Run transaction on the ticket node to decrement onHold and increment available
                            ticketsRef.child(key).runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                    TicketFormData tLocal = currentData.getValue(TicketFormData.class);
                                    if (tLocal == null) return Transaction.abort();

                                    tLocal.setOnHoldQuantity(Math.max(0, tLocal.getOnHoldQuantity() - qty));
                                    tLocal.setAvailableQuantity(tLocal.getAvailableQuantity() + qty);
                                    currentData.setValue(tLocal);
                                    return Transaction.success(currentData);
                                }

                                @Override
                                public void onComplete(@NonNull DatabaseError error, boolean committed, DataSnapshot currentData) {
                                    // After transaction completes, remove the hold
                                    holdSnap.getRef().removeValue();
                                }
                            });
                        } else if (expiresAt != null && expiresAt <= now) {
                            // Even if qty is null, remove the hold
                            holdSnap.getRef().removeValue();
                        }
                    }
                }
            }

            if (onComplete != null) onComplete.run();
        }).addOnFailureListener(e -> {
            Toast.makeText(TicketBookingActivity.this, "Failed to cleanup expired holds: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (onComplete != null) onComplete.run();
        });
    }


}
