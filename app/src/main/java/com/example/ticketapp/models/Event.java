package com.example.ticketapp.models;

import java.util.List;
import java.util.UUID;

public class Event {

    private String eventId;
    private String title;
    private String date;
    private String details;
    private List<TicketFormData> tickets;

    // Sale times in milliseconds
    private long saleStartTime;
    private long saleEndTime;

    public Event() {} // Firebase requirement

    // Constructor for new events — generates a unique eventId automatically
    public Event(String title, String date, String details, List<TicketFormData> tickets,
                 long saleStartTime, long saleEndTime) {
        this.eventId = generateUniqueId();
        this.title = title;
        this.date = date;
        this.details = details;
        this.tickets = tickets;
        this.saleStartTime = saleStartTime;
        this.saleEndTime = saleEndTime;
    }

    // Optional constructor if you already have an eventId (e.g., loading from Firebase)
    public Event(String eventId, String title, String date, String details, List<TicketFormData> tickets,
                 long saleStartTime, long saleEndTime) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.details = details;
        this.tickets = tickets;
        this.saleStartTime = saleStartTime;
        this.saleEndTime = saleEndTime;
    }

    private String generateUniqueId() {
        // Short 8-character unique ID
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // --- Getters ---
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getDetails() { return details; }
    public List<TicketFormData> getTickets() { return tickets; }
    public long getSaleStartTime() { return saleStartTime; }
    public long getSaleEndTime() { return saleEndTime; }

    // --- Setters ---
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setDetails(String details) { this.details = details; }
    public void setTickets(List<TicketFormData> tickets) { this.tickets = tickets; }
    public void setSaleStartTime(long saleStartTime) { this.saleStartTime = saleStartTime; }
    public void setSaleEndTime(long saleEndTime) { this.saleEndTime = saleEndTime; }

    // --- Total remaining tickets ---
    public int getTotalRemainingTickets() {
        if (tickets == null) return 0;
        int total = 0;
        for (TicketFormData t : tickets) total += t.getQuantity();
        return total;
    }
}
