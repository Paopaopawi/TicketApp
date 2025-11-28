package com.example.ticketapp.models;

import android.os.Parcel;
import android.os.Parcelable;

public class TicketFormData implements Parcelable {
    private String name;
    private String type;
    private int price;
    private int availableQuantity;
    private int onHoldQuantity;
    private String holdId;
    private String eventId;
    private String ticketKey;
    private long holdExpiresAt;

    public TicketFormData() {}

    public TicketFormData(String name, String type, int price, int availableQuantity, long expiresAt) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.availableQuantity = availableQuantity;
        this.onHoldQuantity = 0;
    }

    // Parcelable constructor
    protected TicketFormData(Parcel in) {
        name = in.readString();
        type = in.readString();
        price = in.readInt();
        availableQuantity = in.readInt();
        onHoldQuantity = in.readInt();
        holdId = in.readString();
        eventId = in.readString();
        ticketKey = in.readString();
        holdExpiresAt = in.readLong();
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(type);
        dest.writeInt(price);
        dest.writeInt(availableQuantity);
        dest.writeInt(onHoldQuantity);
        dest.writeString(holdId);
        dest.writeString(eventId);
        dest.writeString(ticketKey);
        dest.writeLong(holdExpiresAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    public int getOnHoldQuantity() { return onHoldQuantity; }
    public void setOnHoldQuantity(int onHoldQuantity) { this.onHoldQuantity = onHoldQuantity; }

    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTicketKey() { return ticketKey; }
    public void setTicketKey(String ticketKey) { this.ticketKey = ticketKey; }

    public long getExpiresAt() { return holdExpiresAt; }
    public void setExpiresAt(long holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
}
