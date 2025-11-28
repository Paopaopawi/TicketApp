package com.example.ticketapp.models;

public class PaymentTicket {
    private String name;
    private String type;
    private int price;
    private int quantity;

    public PaymentTicket() {}
    public PaymentTicket(String name, String type, int price, int quantity){
        this.name = name;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters & setters
    public String getName(){return name;}
    public String getType(){return type;}
    public int getPrice(){return price;}
    public int getQuantity(){return quantity;}

    public void setName(String n){this.name=n;}
    public void setType(String t){this.type=t;}
    public void setPrice(int p){this.price=p;}
    public void setQuantity(int q){this.quantity=q;}
}
