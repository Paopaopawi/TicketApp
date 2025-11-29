package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
    private Map<String, String> ticketNameToKey = new HashMap<>();
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
            cleanupExpiredHolds(() -> {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                checkUserActiveHold(uid, (hasActive, nearestExpiry) -> {
                    if (hasActive) {
                        long now = System.currentTimeMillis();
                        long remaining = nearestExpiry - now;
                        long sec = remaining / 1000;
                        long min = sec / 60;
                        sec %= 60;

                        Toast.makeText(
                                TicketBookingActivity.this,
                                "You can only book once every 5 minutes.\nWait " + min + "m " + sec + "s before booking again.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        holdAndProceed();
                    }
                });
            });
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
            int qty = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                qty = selectedQuantities.getOrDefault(t.getName(), 0);
            }
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
        final List<TicketFormData> heldTicketsForIntent = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : toHold.entrySet()) {
            String ticketName = entry.getKey();
            int qty = entry.getValue();
            String key = ticketNameToKey.get(ticketName);
            if (key == null) continue;

            ticketsRef.child(key).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    TicketFormData t = currentData.getValue(TicketFormData.class);
                    if (t == null) return Transaction.abort();

                    int freeQty = t.getAvailableQuantity();
                    if (freeQty < qty) return Transaction.abort();

                    MutableData holdsNode = currentData.child("holds");

                    // create per-hold expiresAt in memory only
                    long holdExpiresAt = System.currentTimeMillis() + (5 * 60 * 1000L);

                    // create hold node in Firebase
                    String newHoldId = UUID.randomUUID().toString();
                    Map<String, Object> holdData = new HashMap<>();
                    holdData.put("userId", uid);
                    holdData.put("quantity", qty);
                    holdData.put("expiresAt", holdExpiresAt);
                    holdsNode.child(newHoldId).setValue(holdData);

                    // update ticket quantities
                    t.setAvailableQuantity(freeQty - qty);
                    t.setOnHoldQuantity(t.getOnHoldQuantity() + qty);
                    currentData.child("availableQuantity").setValue(t.getAvailableQuantity());
                    currentData.child("onHoldQuantity").setValue(t.getOnHoldQuantity());

                    // create checkout copy
                    TicketFormData copy = new TicketFormData(
                            t.getName(),
                            t.getType(),
                            t.getPrice(),
                            t.getAvailableQuantity(),
                            0
                    );

                    copy.setName(t.getName());
                    copy.setType(t.getType());
                    copy.setPrice(t.getPrice());
                    copy.setOnHoldQuantity(qty);
                    copy.setEventId(eventId);
                    copy.setTicketKey(key);
                    copy.setHoldId(newHoldId);
                    copy.setPerHoldExpiry(holdExpiresAt);

                    heldTicketsForIntent.add(copy);

                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@NonNull DatabaseError error, boolean committed, DataSnapshot currentData) {
                    if (!committed) {
                        Toast.makeText(TicketBookingActivity.this, "Failed to hold tickets", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (heldTicketsForIntent.size() == toHold.size()) {
                        Intent it = new Intent(TicketBookingActivity.this, CheckOutActivity.class);
                        it.putParcelableArrayListExtra("selectedTickets", new ArrayList<>(heldTicketsForIntent));
                        it.putExtra("eventId", eventId);
                        startActivity(it);
                    }
                }
            });
        }
    }

    private void cleanupExpiredHolds(Runnable onComplete) {
        DatabaseReference ticketsRef = FirebaseHelper.getEventsRef().child(eventId).child("tickets");
        long now = System.currentTimeMillis();

        ticketsRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot ticketSnap : snapshot.getChildren()) {
                String ticketKey = ticketSnap.getKey();
                if (ticketKey == null) continue;

                ticketsRef.child(ticketKey).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        TicketFormData ticketData = currentData.getValue(TicketFormData.class);
                        if (ticketData == null) return Transaction.success(currentData);

                        MutableData holdsNode = currentData.child("holds");
                        if (holdsNode.getValue() != null) {
                            int activeOnHold = 0;
                            List<String> expiredHoldKeys = new ArrayList<>();

                            for (MutableData holdSnap : holdsNode.getChildren()) {
                                Long holdExpiry = holdSnap.child("expiresAt").getValue(Long.class);
                                Integer qty = holdSnap.child("quantity").getValue(Integer.class);
                                if (qty == null) qty = 0;

                                if (holdExpiry == null || holdExpiry <= now) {
                                    expiredHoldKeys.add(holdSnap.getKey());
                                } else {
                                    activeOnHold += qty;
                                }
                            }

                            for (String expiredKey : expiredHoldKeys) {
                                holdsNode.child(expiredKey).setValue(null);
                            }

                            int totalTickets = ticketData.getAvailableQuantity() + ticketData.getOnHoldQuantity();
                            int newAvailable = Math.max(totalTickets - activeOnHold, 0);

                            currentData.child("availableQuantity").setValue(newAvailable);
                            currentData.child("onHoldQuantity").setValue(activeOnHold);
                        }

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@NonNull DatabaseError error, boolean committed, DataSnapshot currentData) { }
                });
            }

            if (onComplete != null) onComplete.run();
        }).addOnFailureListener(e -> {
            if (onComplete != null) onComplete.run();
        });
    }

    private void checkUserActiveHold(String uid, ActiveHoldCallback callback) {
        DatabaseReference ticketsRef = FirebaseHelper.getEventsRef().child(eventId).child("tickets");

        ticketsRef.get().addOnSuccessListener(snapshot -> {
            long now = System.currentTimeMillis();
            boolean hasActiveHold = false;
            long nearestExpiry = Long.MAX_VALUE;

            for (DataSnapshot ticketSnap : snapshot.getChildren()) {
                DataSnapshot holdsSnap = ticketSnap.child("holds");
                for (DataSnapshot holdSnap : holdsSnap.getChildren()) {
                    String holdUser = holdSnap.child("userId").getValue(String.class);
                    Long expiresAt = holdSnap.child("expiresAt").getValue(Long.class);
                    if (holdUser != null && holdUser.equals(uid) && expiresAt != null && expiresAt > now) {
                        hasActiveHold = true;
                        nearestExpiry = Math.min(nearestExpiry, expiresAt);
                    }
                }
            }

            callback.onCheck(hasActiveHold, nearestExpiry);
        }).addOnFailureListener(e -> callback.onCheck(false, 0));
    }

    private interface ActiveHoldCallback {
        void onCheck(boolean hasActiveHold, long nearestExpiry);
    }
}
