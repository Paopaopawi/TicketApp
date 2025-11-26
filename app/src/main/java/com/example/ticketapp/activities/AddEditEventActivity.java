package com.example.ticketapp.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketapp.R;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.models.TicketFormData;
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditEventActivity extends AppCompatActivity {

    private TextInputLayout tilEventName, tilEventDate, tilEventVenue;
    private TextInputEditText etEventName, etEventDate, etEventVenue;
    private Button btnSaveEvent, btnAddTicket;
    private LinearLayout ticketsContainer;

    private String eventId; // null for add, has value for edit
    private List<TicketForm> ticketForms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        tilEventName = findViewById(R.id.tilEventName);
        tilEventDate = findViewById(R.id.tilEventDate);
        tilEventVenue = findViewById(R.id.tilEventVenue);

        etEventName = findViewById(R.id.etEventName);
        etEventDate = findViewById(R.id.etEventDate);
        etEventVenue = findViewById(R.id.etEventVenue);

        ticketsContainer = findViewById(R.id.ticketsContainer);
        btnSaveEvent = findViewById(R.id.btnSaveEvent);
        btnAddTicket = findViewById(R.id.btnAddTicket);

        // date picker
        etEventDate.setOnClickListener(v -> showDatePicker());

        // add first ticket form by default
        addTicketForm(null);

        btnAddTicket.setOnClickListener(v -> addTicketForm(null));

        btnSaveEvent.setOnClickListener(v -> saveEvent());

        // check if editing
        eventId = getIntent().getStringExtra("eventId");
        if(eventId != null){
            loadEventData();
        }
    }

    private void showDatePicker(){
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format("%02d/%02d/%d", dayOfMonth, month+1, year);
            etEventDate.setText(dateStr);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addTicketForm(TicketFormData data){
        TicketForm ticketForm = new TicketForm(this, ticketsContainer, data);
        ticketsContainer.addView(ticketForm);
        ticketForms.add(ticketForm);
    }

    private void loadEventData(){
        FirebaseHelper.getEventsRef().child(eventId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Event event = snapshot.getValue(Event.class);
                        if(event != null){
                            etEventName.setText(event.getTitle());
                            etEventDate.setText(event.getDate());
                            etEventVenue.setText(event.getLocation());

                            // clear default ticket form
                            ticketsContainer.removeAllViews();
                            ticketForms.clear();

                            // load tickets
                            for(TicketFormData t : event.getTickets()){
                                addTicketForm(t);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddEditEventActivity.this, "Failed to load event: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveEvent(){
        tilEventName.setError(null);
        tilEventDate.setError(null);
        tilEventVenue.setError(null);

        String name = etEventName.getText() != null ? etEventName.getText().toString().trim() : "";
        String date = etEventDate.getText() != null ? etEventDate.getText().toString().trim() : "";
        String venue = etEventVenue.getText() != null ? etEventVenue.getText().toString().trim() : "";

        boolean valid = true;
        if(name.isEmpty()){ tilEventName.setError("Event name required"); valid=false;}
        if(date.isEmpty()){ tilEventDate.setError("Date required"); valid=false;}
        if(venue.isEmpty()){ tilEventVenue.setError("Venue required"); valid=false;}
        if(!valid) return;

        List<TicketFormData> ticketsData = new ArrayList<>();
        for(TicketForm tf : ticketForms){
            TicketFormData t = tf.getData();
            if(t == null) return; // error already set inside TicketForm
            ticketsData.add(t);
        }

        String id = eventId != null ? eventId : FirebaseHelper.getEventsRef().push().getKey();
        Event event = new Event(id, name, date, venue, ticketsData);

        FirebaseHelper.getEventsRef().child(id).setValue(event)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(AddEditEventActivity.this, "Event saved", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddEditEventActivity.this, "Error: "+task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
