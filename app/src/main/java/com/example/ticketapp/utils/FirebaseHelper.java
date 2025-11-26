package com.example.ticketapp.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    private static FirebaseDatabase database = FirebaseDatabase.getInstance();

    public static DatabaseReference getUsersRef() { return database.getReference("users"); }
    public static DatabaseReference getEventsRef() { return database.getReference("events"); }
    public static DatabaseReference getReservationsRef() { return database.getReference("reservations"); }
}

