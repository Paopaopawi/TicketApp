package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.List;

public class EventListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button btnAddEvent;
    private List<Event> eventList;
    private EventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        Log.d("EventList", "onCreate reached");

        recyclerView = findViewById(R.id.recyclerViewEvents);
        btnAddEvent = findViewById(R.id.btnAddEvent);

        eventList = new ArrayList<>();
        adapter = new EventAdapter(this, eventList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnAddEvent.setOnClickListener(v -> {
            startActivity(new Intent(EventListActivity.this, AddEditEventActivity.class));
        });

        loadEventsFromFirebase();
    }

    private void loadEventsFromFirebase() {
        FirebaseHelper.getEventsRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event event = ds.getValue(Event.class);
                    if (event != null) eventList.add(event);
                }
                adapter.notifyDataSetChanged();
                Log.d("EventList", "Events loaded: " + eventList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EventListActivity.this, "Failed to load events: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
