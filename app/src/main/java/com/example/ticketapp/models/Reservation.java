package com.example.ticketapp.models;
public class Reservation {
    private String reservationId, userId, eventId, seatNumber;

    public Reservation() {}

    public Reservation(String reservationId, String userId, String eventId, String seatNumber) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.eventId = eventId;
        this.seatNumber = seatNumber;
    }

    public String getReservationId() { return reservationId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public String getSeatNumber() { return seatNumber; }
}

