package com.example.ticketapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.PaymentSuccessAdapter;
import com.example.ticketapp.models.TicketFormData;

import java.util.ArrayList;

public class PaymentSuccess extends AppCompatActivity {

    private TextView tvPaymentSummary;
    private RecyclerView rvPurchasedTickets;
    private Button btnViewMyTickets, btnBackToEvents;

    private ArrayList<TicketFormData> purchasedTickets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        tvPaymentSummary = findViewById(R.id.tvPaymentSummary);
        rvPurchasedTickets = findViewById(R.id.rvPurchasedTickets);
        btnViewMyTickets = findViewById(R.id.btnViewMyTickets);
        btnBackToEvents = findViewById(R.id.btnBackToEvents);

        // Retrieve totals and tickets from Intent
        int subtotal = getIntent().getIntExtra("subtotal", 0);
        int serviceFee = getIntent().getIntExtra("serviceFee", 0);
        int total = getIntent().getIntExtra("total", 0);

        purchasedTickets = getIntent().getParcelableArrayListExtra("tickets");
        if (purchasedTickets == null) purchasedTickets = new ArrayList<>();

        // Display totals
        tvPaymentSummary.setText(
                "Subtotal: ₱" + subtotal + "\n" +
                        "Service Fee (2.5%): ₱" + serviceFee + "\n" +
                        "TOTAL PAID: ₱" + total
        );

        // Setup RecyclerView with adapter
        PaymentSuccessAdapter adapter = new PaymentSuccessAdapter(purchasedTickets);
        rvPurchasedTickets.setLayoutManager(new LinearLayoutManager(this));
        rvPurchasedTickets.setAdapter(adapter);

        // Button listeners
        btnViewMyTickets.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.ticketapp.activities.UserDashboardActivity.class));
            finish();
        });

        btnBackToEvents.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.ticketapp.activities.UserDashboardActivity.class));
            finish();
        });
    }
}
