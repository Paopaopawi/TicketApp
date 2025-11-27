package com.example.ticketapp.models;

import android.os.Parcel;
import android.os.Parcelable;

public class TicketFormData implements Parcelable {

    private String name;
    private String type;
    private int price;
    private int quantity;

    // Default constructor required for Firebase
    public TicketFormData() {}

    public TicketFormData(String name, String type, int price, int quantity) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }

    // --- Parcelable constructor ---
    protected TicketFormData(Parcel in) {
        name = in.readString();
        type = in.readString();
        price = in.readInt();
        quantity = in.readInt();
    }

    public static final Creator<TicketFormData> CREATOR = new Creator<TicketFormData>() {
        @Override
        public TicketFormData createFromParcel(Parcel in) {
            return new TicketFormData(in);
        }

        @Override
        public TicketFormData[] newArray(int size) {
            return new TicketFormData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(type);
        parcel.writeInt(price);
        parcel.writeInt(quantity);
    }

    // --- Getters ---
    public String getName() { return name; }
    public String getType() { return type; }
    public int getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getTypeName() { return type; }

    // --- Setters ---
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setPrice(int price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
