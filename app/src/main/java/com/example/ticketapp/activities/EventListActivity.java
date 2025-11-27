package com.example.ticketapp.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.EventAdapter;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private Spinner spFilter;
    private Button btnAddEvent;

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Object> displayList = new ArrayList<>();
    private EventAdapter adapter;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        recyclerView = findViewById(R.id.recyclerViewEvents);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        etSearch = findViewById(R.id.etSearch);
        spFilter = findViewById(R.id.spFilter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(this, displayList,
                this::onEventClick, this::onEditClick, this::onDeleteClick);
        recyclerView.setAdapter(adapter);

        setupFilterDropdown();
        setupSearch();

        btnAddEvent.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditEventActivity.class))
        );

        loadEvents();
    }

    private void setupFilterDropdown() {
        String[] filterOptions = {"All", "Ongoing", "Upcoming", "Past"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spFilter.setAdapter(spinnerAdapter);

        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(android.text.Editable s) { }
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
                Toast.makeText(EventListActivity.this, "Error: "+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        String search = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        String filter = spFilter.getSelectedItem() != null ? spFilter.getSelectedItem().toString() : "All";

        List<Event> filtered = new ArrayList<>();
        for (Event e : allEvents) {
            if (e == null) continue;
            boolean matchesSearch = (e.getTitle() != null && e.getTitle().toLowerCase().contains(search))
                    || (e.getDetails() != null && e.getDetails().toLowerCase().contains(search));
            boolean matchesFilter = filter.equals("All") ||
                    (filter.equalsIgnoreCase("Ongoing") && isOngoing(e)) ||
                    (filter.equalsIgnoreCase("Upcoming") && isUpcoming(e)) ||
                    (filter.equalsIgnoreCase("Past") && isPast(e));

            if (matchesSearch && matchesFilter) filtered.add(e);
        }

        populateSections(filtered);
    }

    private void populateSections(List<Event> events) {
        displayList.clear();
        List<Event> ongoing = new ArrayList<>();
        List<Event> upcoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();

        for (Event e : events) {
            if (isOngoing(e)) ongoing.add(e);
            else if (isUpcoming(e)) upcoming.add(e);
            else past.add(e);
        }

        if(!ongoing.isEmpty()){ displayList.add("Ongoing Events"); displayList.addAll(ongoing);}
        if(!upcoming.isEmpty()){ displayList.add("Upcoming Events"); displayList.addAll(upcoming);}
        if(!past.isEmpty()){ displayList.add("Past Events"); displayList.addAll(past);}

        adapter.notifyDataSetChanged();
    }

    private boolean isOngoing(Event e){
        Date today = clearTime(new Date());
        Date eventDate = parseDate(e.getDate());
        return eventDate != null && eventDate.equals(today);
    }

    private boolean isUpcoming(Event e){
        Date today = clearTime(new Date());
        Date eventDate = parseDate(e.getDate());
        return eventDate != null && eventDate.after(today);
    }

    private boolean isPast(Event e){
        Date today = clearTime(new Date());
        Date eventDate = parseDate(e.getDate());
        return eventDate != null && eventDate.before(today);
    }

    private Date parseDate(String s){
        try{ return sdf.parse(s); } catch(Exception ex){ return null; }
    }

    private Date clearTime(Date d){
        if(d == null) return null;
        Calendar c = Calendar.getInstance();
        c.setTime(d); c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c.getTime();
    }

    private void onEventClick(Event e){
        // Show popup with details
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(e.getTitle()).append("\n");
        sb.append("Date: ").append(e.getDate()).append("\n");
        sb.append("Details: ").append(e.getDetails()).append("\n");
        sb.append("Tickets Remaining: ").append(e.getTotalRemainingTickets()).append("\n");

        new AlertDialog.Builder(this)
                .setTitle("Event Details")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void onEditClick(Event e){
        Intent intent = new Intent(this, AddEditEventActivity.class);
        intent.putExtra("eventId", e.getEventId());
        startActivity(intent);
    }

    private void onDeleteClick(Event e){
        new AlertDialog.Builder(this)
                .setTitle("Delete Event?")
                .setMessage("Are you sure you want to delete " + e.getTitle() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseHelper.getEventsRef().child(e.getEventId()).removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
