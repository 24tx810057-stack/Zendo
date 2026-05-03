package com.example.buoi1;

import java.io.Serializable;
import java.util.List;

public class WarrantyRequest implements Serializable {
    private String id;
    private String orderId;
    private String productId;
    private String userEmail;
    private String errorType;
    private String description;
    private List<String> evidenceImages;
    private String status; // pending_repair, repairing, repaired, rejected
    private String type;
    private long timestamp;

    public WarrantyRequest() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getEvidenceImages() { return evidenceImages; }
    public void setEvidenceImages(List<String> evidenceImages) { this.evidenceImages = evidenceImages; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
