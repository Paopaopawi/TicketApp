package com.example.ticketapp.models;

public class TicketFormData {

    private String name;
    private String type;
    private int price;
    private int quantity;

    public TicketFormData() {
        // required for Firebase
    }

    public TicketFormData(String name, String type, int price, int quantity) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }

    // GETTERS
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getTypeName() {
        return type;
    }

    // SETTERS (firebase needs these)
    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
