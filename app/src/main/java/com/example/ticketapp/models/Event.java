package com.example.ticketapp.models;

import java.util.List;

public class Event {

    private String eventId;
    private String title;
    private String date;
    private String location;

    private List<TicketFormData> tickets;

    public Event() {}  // Required for Firebase

    public Event(String eventId, String title, String date, String location, List<TicketFormData> tickets) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.location = location;
        this.tickets = tickets;
    }

    // GETTERS
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getLocation() { return location; }
    public List<TicketFormData> getTickets() { return tickets; }

    // SETTERS
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setLocation(String location) { this.location = location; }
    public void setTickets(List<TicketFormData> tickets) { this.tickets = tickets; }

    // NEW: total remaining tickets
    public int getTotalRemainingTickets() {
        if (tickets == null) return 0;

        int total = 0;
        for (TicketFormData t : tickets) {
            total += t.getQuantity();
        }
        return total;
    }
}
