package com.example.ticketapp.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Event {

    private String eventId;
    private String title;
    private String date; // format "dd/MM/yyyy"
    private String details;
    private List<TicketFormData> tickets;
    private long saleStartTime;
    private long saleEndTime;

    public Event() {} // Firebase

    public Event(String eventId, String title, String date, String details,
                 List<TicketFormData> tickets, long saleStartTime, long saleEndTime) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.details = details;
        this.tickets = tickets;
        this.saleStartTime = saleStartTime;
        this.saleEndTime = saleEndTime;
    }

    // Getters / Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public List<TicketFormData> getTickets() { return tickets; }

    public void setTickets(Object ticketsObj) {
        if (ticketsObj instanceof List) {
            this.tickets = (List<TicketFormData>) ticketsObj;
        } else if (ticketsObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) ticketsObj;
            this.tickets = new ArrayList<>();
            for (Object value : map.values()) {
                if (value instanceof Map) {
                    Map<String, Object> ticketMap = (Map<String, Object>) value;
                    TicketFormData t = new TicketFormData();
                    t.setName(ticketMap.get("name") != null ? (String) ticketMap.get("name") : "");
                    t.setType(ticketMap.get("type") != null ? (String) ticketMap.get("type") : "");

                    Object priceObj = ticketMap.get("price");
                    t.setPrice(priceObj != null ?
                            (priceObj instanceof Long ? ((Long) priceObj).intValue() : (Integer) priceObj)
                            : 0);

                    Object availableObj = ticketMap.get("availableQuantity");
                    t.setAvailableQuantity(availableObj != null ?
                            (availableObj instanceof Long ? ((Long) availableObj).intValue() : (Integer) availableObj)
                            : 0);

                    Object onHoldObj = ticketMap.get("onHoldQuantity");
                    t.setOnHoldQuantity(onHoldObj != null ?
                            (onHoldObj instanceof Long ? ((Long) onHoldObj).intValue() : (Integer) onHoldObj)
                            : 0);

                    this.tickets.add(t);
                }
            }
        } else {
            this.tickets = new ArrayList<>();
        }
    }


    public long getSaleStartTime() { return saleStartTime; }
    public void setSaleStartTime(long saleStartTime) { this.saleStartTime = saleStartTime; }

    public long getSaleEndTime() { return saleEndTime; }
    public void setSaleEndTime(long saleEndTime) { this.saleEndTime = saleEndTime; }

    // convenience
    public int getTotalRemainingTickets() {
        if (tickets == null) return 0;
        int total = 0;
        for (TicketFormData t : tickets) {
            total += Math.max(0, t.getAvailableQuantity() - t.getOnHoldQuantity());
        }
        return total;
    }
}
