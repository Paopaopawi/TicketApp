package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.ticketapp.R;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardEvent, cardReservation, cardUser, cardReports, cardNotifications, cardProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Find CardViews
        cardEvent = findViewById(R.id.cardEventManagement);
        cardReservation = findViewById(R.id.cardReservationManagement);
        cardUser = findViewById(R.id.cardUserManagement);
        cardReports = findViewById(R.id.cardReports);
        cardNotifications = findViewById(R.id.cardNotifications);
        cardProfile = findViewById(R.id.cardProfile);

        // Set click listeners
        cardEvent.setOnClickListener(v -> openActivity(EventListActivity.class));
        cardReservation.setOnClickListener(v -> openActivity(ReservationListActivity.class));
        cardUser.setOnClickListener(v -> openActivity(UserListActivity.class));
        cardReports.setOnClickListener(v -> openActivity(ReportsActivity.class));
        cardNotifications.setOnClickListener(v -> openActivity(NotificationsActivity.class));
        cardProfile.setOnClickListener(v -> openActivity(AdminProfileActivity.class));
    }

    private void openActivity(Class<?> activityClass){
        startActivity(new Intent(AdminDashboardActivity.this, activityClass));
    }
}
