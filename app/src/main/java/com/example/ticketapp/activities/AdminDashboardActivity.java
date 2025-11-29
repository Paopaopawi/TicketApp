package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.ticketapp.R;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardEvent, cardReservation, cardReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Find CardViews
        cardEvent = findViewById(R.id.cardEventManagement);
        cardReports = findViewById(R.id.cardReports);

        // Set click listeners
        cardEvent.setOnClickListener(v -> openActivity(EventListActivity.class));
    }

    private void openActivity(Class<?> activityClass){
        startActivity(new Intent(AdminDashboardActivity.this, activityClass));
    }
}
