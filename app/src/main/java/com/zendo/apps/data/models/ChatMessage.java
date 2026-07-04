package com.zendo.apps.data.models;

public class ChatMessage {
    private String message;
    private String sender; // "user" hoặc "admin"
    private long timestamp;
    private boolean isAutoReply;

    public ChatMessage() {}

    public ChatMessage(String message, String sender, long timestamp) {
        this(message, sender, timestamp, false);
    }

    public ChatMessage(String message, String sender, long timestamp, boolean isAutoReply) {
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
        this.isAutoReply = isAutoReply;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isAutoReply() { return isAutoReply; }
    public void setAutoReply(boolean autoReply) { isAutoReply = autoReply; }
}
