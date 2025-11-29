package com.example.ticketapp.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.adapters.PaymentSuccessAdapter; // reuse adapter
import com.example.ticketapp.models.TicketFormData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.example.ticketapp.utils.FirebaseHelper;

import java.util.ArrayList;

public class UserTicketsActivity extends AppCompatActivity {

    private RecyclerView rvUserTickets;
    private TextView tvNoTickets;

    private ArrayList<TicketFormData> userTickets = new ArrayList<>();
    private PaymentSuccessAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_tickets);

        rvUserTickets = findViewById(R.id.rvUserTickets);
        tvNoTickets = findViewById(R.id.tvNoTickets);

        adapter = new PaymentSuccessAdapter(userTickets);
        rvUserTickets.setLayoutManager(new LinearLayoutManager(this));
        rvUserTickets.setAdapter(adapter);

        loadUserTickets();
    }

    private void loadUserTickets() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference bookingsRef = FirebaseHelper.getRootRef().child("bookings");

        bookingsRef.orderByChild("userId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        userTickets.clear();

                        for (DataSnapshot bookingSnap : snapshot.getChildren()) {
                            DataSnapshot ticketsNode = bookingSnap.child("tickets");
                            for (DataSnapshot ticketSnap : ticketsNode.getChildren()) {
                                TicketFormData t = new TicketFormData();
                                t.setName(ticketSnap.child("ticketName").getValue(String.class));
                                t.setPrice(ticketSnap.child("price").getValue(Integer.class) != null ?
                                        ticketSnap.child("price").getValue(Integer.class) : 0);
                                t.setOnHoldQuantity(ticketSnap.child("quantity").getValue(Integer.class) != null ?
                                        ticketSnap.child("quantity").getValue(Integer.class) : 0);
                                t.setTicketCode(ticketSnap.child("code").getValue(String.class));
                                userTickets.add(t);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        tvNoTickets.setVisibility(userTickets.isEmpty() ? TextView.VISIBLE : TextView.GONE);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvNoTickets.setText("Failed to load tickets");
                        tvNoTickets.setVisibility(TextView.VISIBLE);
                    }
                });
    }
}
