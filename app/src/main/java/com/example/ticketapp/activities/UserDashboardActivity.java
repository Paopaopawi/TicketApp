package com.example.ticketapp.activities;


import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import com.example.ticketapp.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // TODO: Load list of events using RecyclerView
        Toast.makeText(this, "User Dashboard loaded (events list to be implemented)", Toast.LENGTH_SHORT).show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("test");

        ref.setValue("Hello Firebase!")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Connected to Firebase!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

    }
}

