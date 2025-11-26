package com.example.ticketapp.models;

public class Ticket {
    private String name;
    private String type;
    private String details;
    private int price;

    public Ticket() {} // Required for Firebase

    public Ticket(String name, String type, int price, String details) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.details = details;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getPrice() { return price; }
    public String getDetails() { return details; }

    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setPrice(int price) { this.price = price; }
    public void setDetails(String details) { this.details = details; }
}
