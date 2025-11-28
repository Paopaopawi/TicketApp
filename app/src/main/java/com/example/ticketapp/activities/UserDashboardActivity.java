package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.UserEventAdapter;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.models.User;
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private Spinner spFilter;

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Object> displayList = new ArrayList<>();
    private UserEventAdapter adapter;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private final long BOOKING_COOLDOWN = 10 * 60 * 1000; // 10 minutes in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        recyclerView = findViewById(R.id.recyclerViewEvents);
        etSearch = findViewById(R.id.etSearch);
        spFilter = findViewById(R.id.spFilter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserEventAdapter(this, displayList, false); // false = user
        recyclerView.setAdapter(adapter);

        setupFilterDropdown();
        setupSearch();
        loadEvents();
        setupEventClick();
    }

    private void setupFilterDropdown() {
        String[] options = {"All", "Ongoing", "Incoming", "Past"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        spFilter.setAdapter(spinnerAdapter);
        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadEvents() {
        FirebaseHelper.getEventsRef().addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEvents.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event e = ds.getValue(Event.class);
                    if (e != null) allEvents.add(e);
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserDashboardActivity.this, "Error loading events: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        String search = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        String filter = spFilter.getSelectedItem() != null ? spFilter.getSelectedItem().toString() : "All";

        List<Event> filtered = new ArrayList<>();
        for (Event e : allEvents) {
            if (e == null) continue;
            boolean matchesSearch = (e.getTitle() != null && e.getTitle().toLowerCase().contains(search))
                    || (e.getDetails() != null && e.getDetails().toLowerCase().contains(search));

            boolean matchesFilter;
            switch (filter) {
                case "Ongoing": matchesFilter = isOngoing(e); break;
                case "Incoming": matchesFilter = isIncoming(e); break;
                case "Past": matchesFilter = isPast(e); break;
                default: matchesFilter = true;
            }

            if (matchesSearch && matchesFilter) filtered.add(e);
        }

        populateDisplayList(filtered);
    }

    private void populateDisplayList(List<Event> list) {
        displayList.clear();
        List<Event> ongoing = new ArrayList<>();
        List<Event> incoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();

        for (Event e : list) {
            if (isOngoing(e)) ongoing.add(e);
            else if (isIncoming(e)) incoming.add(e);
            else past.add(e);
        }

        if (!ongoing.isEmpty()) {
            displayList.add("Ongoing Events");
            displayList.addAll(ongoing);
        }
        if (!incoming.isEmpty()) {
            displayList.add("Incoming Events");
            displayList.addAll(incoming);
        }
        if (!past.isEmpty()) {
            displayList.add("Past Events");
            displayList.addAll(past);
        }

        adapter.notifyDataSetChanged();
    }

    private Date clearTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTime();
    }

    private boolean isOngoing(Event e) {
        try {
            Date today = clearTime(new Date());
            Date eventDate = clearTime(sdf.parse(e.getDate()));
            return today.equals(eventDate);
        } catch (Exception ex) { return false; }
    }

    private boolean isIncoming(Event e) {
        try {
            Date today = clearTime(new Date());
            Date eventDate = clearTime(sdf.parse(e.getDate()));
            return eventDate.after(today);
        } catch (Exception ex) { return false; }
    }

    private boolean isPast(Event e) {
        try {
            Date today = clearTime(new Date());
            Date eventDate = clearTime(sdf.parse(e.getDate()));
            return eventDate.before(today);
        } catch (Exception ex) { return false; }
    }

    // ------------------- NEW FUNCTION -------------------
    private void setupEventClick() {
        adapter.setOnEventClickListener(event -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseHelper.getUsersRef().child(uid).child("lastBookingTimestamp")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            long lastBooking = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                            long now = System.currentTimeMillis();
                            if (now - lastBooking < BOOKING_COOLDOWN) {
                                long minutesLeft = (BOOKING_COOLDOWN - (now - lastBooking)) / 60000;
                                Toast.makeText(UserDashboardActivity.this,
                                        "You can book again in " + minutesLeft + " minutes.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Update last booking timestamp
                                FirebaseHelper.getUsersRef().child(uid).child("lastBookingTimestamp")
                                        .setValue(now);

                                // Go to BookingTicketActivity
                                Intent intent = new Intent(UserDashboardActivity.this, com.example.ticketapp.activities.TicketBookingActivity.class);
                                intent.putExtra("eventId", event.getEventId());
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        });
    }
}
