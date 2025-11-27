package com.example.ticketapp.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditEventActivity extends AppCompatActivity {

    private TextInputLayout tilEventName, tilEventDate, tilEventDetails, tilSaleStart, tilSaleEnd;
    private TextInputEditText etEventName, etEventDate, etEventDetails, etSaleStart, etSaleEnd;

    private Button btnSaveEvent, btnAddTicket;
    private LinearLayout ticketsContainer;

    private String eventId;
    private List<TicketForm> ticketForms = new ArrayList<>();

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        // Initialize views
        tilEventName = findViewById(R.id.tilEventName);
        tilEventDate = findViewById(R.id.tilEventDate);
        tilEventDetails = findViewById(R.id.tilEventDetails);
        tilSaleStart = findViewById(R.id.tilSaleStart);
        tilSaleEnd = findViewById(R.id.tilSaleEnd);

        etEventName = findViewById(R.id.etEventName);
        etEventDate = findViewById(R.id.etEventDate);
        etEventDetails = findViewById(R.id.etEventDetails);
        etSaleStart = findViewById(R.id.etSaleStart);
        etSaleEnd = findViewById(R.id.etSaleEnd);

        ticketsContainer = findViewById(R.id.ticketsContainer);
        btnSaveEvent = findViewById(R.id.btnSaveEvent);
        btnAddTicket = findViewById(R.id.btnAddTicket);

        // Date pickers
        etEventDate.setOnClickListener(v -> showDatePicker(etEventDate));
        etSaleStart.setOnClickListener(v -> showDateTimePicker(etSaleStart));
        etSaleEnd.setOnClickListener(v -> showDateTimePicker(etSaleEnd));

        // Ticket forms
        addTicketForm(null);
        btnAddTicket.setOnClickListener(v -> addTicketForm(null));

        // Save event
        btnSaveEvent.setOnClickListener(v -> saveEvent());

        // Edit mode
        eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) loadEventData();
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                target.setText(String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDateTimePicker(TextInputEditText target){
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(Calendar.YEAR, year);
                picked.set(Calendar.MONTH, month);
                picked.set(Calendar.DAY_OF_MONTH, day);
                picked.set(Calendar.HOUR_OF_DAY, hour);
                picked.set(Calendar.MINUTE, minute);
                picked.set(Calendar.SECOND, 0);
                picked.set(Calendar.MILLISECOND, 0);

                target.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(picked.getTime()));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }


    private void addTicketForm(TicketFormData data) {
        TicketForm ticketForm = new TicketForm(this, ticketsContainer, data);
        ticketsContainer.addView(ticketForm);
        ticketForms.add(ticketForm);
    }

    private void loadEventData() {
        FirebaseHelper.getEventsRef().child(eventId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Event event = snapshot.getValue(Event.class);
                        if (event != null) {
                            etEventName.setText(event.getTitle());
                            etEventDate.setText(event.getDate());
                            etEventDetails.setText(event.getDetails());
                            etSaleStart.setText(sdf.format(new Date(event.getSaleStartTime())));
                            etSaleEnd.setText(sdf.format(new Date(event.getSaleEndTime())));

                            ticketsContainer.removeAllViews();
                            ticketForms.clear();

                            if (event.getTickets() != null) {
                                for (TicketFormData t : event.getTickets()) addTicketForm(t);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddEditEventActivity.this,
                                "Failed to load event: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveEvent() {
        // Reset errors
        tilEventName.setError(null);
        tilEventDate.setError(null);
        tilEventDetails.setError(null);
        tilSaleStart.setError(null);
        tilSaleEnd.setError(null);

        String name = etEventName.getText() != null ? etEventName.getText().toString().trim() : "";
        String date = etEventDate.getText() != null ? etEventDate.getText().toString().trim() : "";
        String details = etEventDetails.getText() != null ? etEventDetails.getText().toString().trim() : "";
        String saleStartStr = etSaleStart.getText() != null ? etSaleStart.getText().toString().trim() : "";
        String saleEndStr = etSaleEnd.getText() != null ? etSaleEnd.getText().toString().trim() : "";

        boolean valid = true;
        if (name.isEmpty()) { tilEventName.setError("Event name required"); valid = false; }
        if (date.isEmpty()) { tilEventDate.setError("Date required"); valid = false; }
        if (details.isEmpty()) { tilEventDetails.setError("Details required"); valid = false; }
        if (saleStartStr.isEmpty()) { tilSaleStart.setError("Sale start required"); valid = false; }
        if (saleEndStr.isEmpty()) { tilSaleEnd.setError("Sale end required"); valid = false; }
        if (!valid) return;

        List<TicketFormData> ticketsData = new ArrayList<>();
        for (TicketForm tf : ticketForms) {
            TicketFormData t = tf.getData();
            if (t == null) return; // error already set inside TicketForm
            ticketsData.add(t);
        }

        // Parse date+time strings into millis with logging
        long saleStart = parseDateTimeWithLogging(saleStartStr, "SaleStart");
        long saleEnd = parseDateTimeWithLogging(saleEndStr, "SaleEnd");

        if (saleStart == 0 || saleEnd == 0) {
            Toast.makeText(this, "Invalid sale start/end time", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = eventId != null ? eventId : FirebaseHelper.getEventsRef().push().getKey();
        Event event = new Event(id, name, date, details, ticketsData, saleStart, saleEnd);

        FirebaseHelper.getEventsRef().child(id).setValue(event)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddEditEventActivity.this, "Event saved", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddEditEventActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Parses a datetime string in format "dd/MM/yyyy HH:mm" and logs millis
     */
    private long parseDateTimeWithLogging(String dateTimeStr, String tag) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            long millis = sdf.parse(dateTimeStr).getTime();
            Log.d("DebugSaleSave", tag + ": " + dateTimeStr + " => millis=" + millis);
            return millis;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DebugSaleSave", tag + ": failed to parse " + dateTimeStr);
            return 0;
        }
    }


    private long parseDateTime(String dateTimeStr){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            sdf.setTimeZone(Calendar.getInstance().getTimeZone()); // use device timezone
            Date d = sdf.parse(dateTimeStr);
            return d.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


}
