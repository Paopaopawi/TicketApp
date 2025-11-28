package com.example.ticketapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.CheckOutAdapter;
import com.example.ticketapp.models.TicketFormData;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckOutActivity extends AppCompatActivity {

    private RecyclerView rvHeldTickets;
    private TextView tvPaymentSubtotal, tvPaymentServiceFee, tvPaymentTotalDue, tvPaymentTimer;
    private Spinner spinnerPaymentMethod;
    private Button btnProceedPayment;

    private List<TicketFormData> heldTickets = new ArrayList<>();
    private CheckOutAdapter adapter;

    private static final double SERVICE_FEE_PERCENT = 0.025;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        rvHeldTickets = findViewById(R.id.rvHeldTickets);
        tvPaymentSubtotal = findViewById(R.id.tvPaymentSubtotal);
        tvPaymentServiceFee = findViewById(R.id.tvPaymentServiceFee);
        tvPaymentTotalDue = findViewById(R.id.tvPaymentTotalDue);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);
        btnProceedPayment = findViewById(R.id.btnProceedPayment);
        tvPaymentTimer = findViewById(R.id.tvPaymentTimer);

        //noinspection unchecked
        heldTickets = getIntent().getParcelableArrayListExtra("selectedTickets");
        if (heldTickets == null) heldTickets = new ArrayList<>();

        if (heldTickets.isEmpty()) {
            Toast.makeText(this, "No tickets to pay for", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new CheckOutAdapter(heldTickets);
        rvHeldTickets.setLayoutManager(new LinearLayoutManager(this));
        rvHeldTickets.setAdapter(adapter);

        calculateTotals();

        btnProceedPayment.setOnClickListener(v -> {
            // integrate real payment here
            Toast.makeText(this, "Payment flow not implemented", Toast.LENGTH_SHORT).show();
        });
    }

    private void calculateTotals() {
        int subtotal = 0;
        for (TicketFormData t : heldTickets) {
            subtotal += t.getOnHoldQuantity() * t.getPrice();
        }
        double serviceFee = subtotal * SERVICE_FEE_PERCENT;
        double total = subtotal + serviceFee;

        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        tvPaymentSubtotal.setText("Subtotal: ₱" + nf.format(subtotal));
        tvPaymentServiceFee.setText("Service Fees (2.5%): ₱" + nf.format(serviceFee));
        tvPaymentTotalDue.setText("TOTAL DUE: ₱" + nf.format(total));
    }
}
