package com.example.buoi1;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notification {
    private String id;
    private String title;
    private String message;
    private String date; 
    private String userEmail;
    private boolean read = false;
    private String type; // Loại thông báo: order_status, promo, ...
    private String orderId; // ID đơn hàng liên quan
    
    private long timestamp; 

    public Notification() {}

    public Notification(String title, String message, String date, String userEmail) {
        this.title = title;
        this.message = message;
        this.date = date;
        this.userEmail = userEmail;
        this.read = false;
        this.timestamp = System.currentTimeMillis();
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
