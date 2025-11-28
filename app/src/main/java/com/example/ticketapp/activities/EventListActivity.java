package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.UserEventAdapter;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.models.TicketFormData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EventListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private Spinner spFilter;
    private Button btnAddEvent;

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Object> displayList = new ArrayList<>();
    private UserEventAdapter adapter;

    private boolean isAdmin = false; // user/admin flag
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        recyclerView = findViewById(R.id.recyclerViewEvents);
        etSearch = findViewById(R.id.etSearch);
        spFilter = findViewById(R.id.spFilter);
        btnAddEvent = findViewById(R.id.btnAddEvent);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddEvent.setVisibility(View.GONE); // hide by default
        btnAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(EventListActivity.this, com.example.ticketapp.activities.AddEditEventActivity.class);
            startActivity(intent);
        });

        adapter = new UserEventAdapter(this, displayList, isAdmin);
        recyclerView.setAdapter(adapter);

        setupSearch();
        setupFilterDropdown();
        loadUserRole(); // fetch role and set access
        loadEvents();
    }

    private void loadUserRole() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseHelper.getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if(user != null){
                            isAdmin = "admin".equals(user.getRole());
                            btnAddEvent.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

                            // Update adapter with admin flag
                            adapter = new UserEventAdapter(EventListActivity.this, displayList, isAdmin);
                            recyclerView.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterDropdown() {
        String[] filterOptions = {"All", "Ongoing", "Upcoming", "Past"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spFilter.setAdapter(spinnerAdapter);
        spFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadEvents() {
        FirebaseHelper.getEventsRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEvents.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event e = ds.getValue(Event.class);
                    if (e != null) {
                        // Convert tickets from List<Map> to List<TicketFormData>
                        if (e.getTickets() != null) {
                            List<TicketFormData> fixedTickets = new ArrayList<>();
                            for (Object o : e.getTickets()) {
                                if (o instanceof HashMap) {
                                    HashMap map = (HashMap) o;
                                    TicketFormData t = new TicketFormData();
                                    t.setType((String) map.get("typeName"));
                                    t.setPrice(map.get("price") != null ? (int) Double.parseDouble(map.get("price").toString()) : 0);
                                    t.setAvailableQuantity((map.get("availableQuantity") != null ? Integer.parseInt(map.get("availableQuantity").toString()) : 0));
                                    t.setOnHoldQuantity((map.get("onHoldQuantity") != null ? Integer.parseInt(map.get("onHoldQuantity").toString()) : 0));
                                    fixedTickets.add(t);
                                }
                            }
                            e.setTickets(fixedTickets);
                        }
                    }
                    allEvents.add(e);
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("EventListDebug", "Firebase error: " + error.getMessage());
            }
        });
    }


    private void applyFilters() {
        String search = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        String filter = spFilter.getSelectedItem() != null ? spFilter.getSelectedItem().toString() : "All";

        List<Event> ongoing = new ArrayList<>();
        List<Event> upcoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();
        Date today = clearTime(new Date());

        for(Event e : allEvents){
            if(e == null) continue;
            boolean matchesSearch = safe(e.getTitle()).contains(search) || safe(e.getDetails()).contains(search);
            boolean matchesFilter = filter.equals("All") || (filter.equals("Ongoing") && isOngoing(e, today))
                    || (filter.equals("Upcoming") && isUpcoming(e, today))
                    || (filter.equals("Past") && isPast(e, today));
            if(matchesSearch && matchesFilter){
                if(isOngoing(e, today)) ongoing.add(e);
                else if(isUpcoming(e, today)) upcoming.add(e);
                else past.add(e);
            }
        }

        displayList.clear();
        if(!ongoing.isEmpty()){ displayList.add("Ongoing Events"); displayList.addAll(ongoing); }
        if(!upcoming.isEmpty()){ displayList.add("Upcoming Events"); displayList.addAll(upcoming); }
        if(!past.isEmpty()){ displayList.add("Past Events"); displayList.addAll(past); }

        adapter.notifyDataSetChanged();
    }

    private boolean isOngoing(Event e, Date today) { return compareDates(e.getDate(), today, 0); }
    private boolean isUpcoming(Event e, Date today) { return compareDates(e.getDate(), today, 1); }
    private boolean isPast(Event e, Date today) { return compareDates(e.getDate(), today, -1); }

    private boolean compareDates(String eventDateStr, Date today, int cmp){
        try{
            if(eventDateStr == null || eventDateStr.trim().isEmpty()) return false;
            Date eventDate = sdf.parse(eventDateStr);
            if(eventDate == null) return false;
            eventDate = clearTime(eventDate);
            int result = eventDate.compareTo(today);
            return cmp==0 ? result==0 : (cmp>0 ? result>0 : result<0);
        }catch(Exception ex){ return false; }
    }

    private Date clearTime(Date d){
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTime();
    }

    private String safe(String s){ return s==null?"":s.toLowerCase(); }
}
