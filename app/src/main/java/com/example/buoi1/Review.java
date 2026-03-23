package com.example.buoi1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Review implements Serializable {
    private String id;
    private String productId;
    private String orderId;
    private String userEmail;
    private String userName;
    private String userAvatar;
    
    private float qualityRating;  
    private float sellerRating;   
    private float shippingRating; 
    
    private String comment;
    private List<String> tags = new ArrayList<>();
    private List<String> mediaUrls = new ArrayList<>();
    private boolean isAnonymous;
    private long timestamp;
    
    // Thêm trường phản hồi từ người bán
    private String sellerReply;
    private long replyTimestamp;

    public Review() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getOrderId() { return orderId; } 
    public void setOrderId(String orderId) { this.orderId = orderId; } 

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public float getQualityRating() { return qualityRating; }
    public void setQualityRating(float qualityRating) { this.qualityRating = qualityRating; }
    public float getSellerRating() { return sellerRating; }
    public void setSellerRating(float sellerRating) { this.sellerRating = sellerRating; }
    public float getShippingRating() { return shippingRating; }
    public void setShippingRating(float shippingRating) { this.shippingRating = shippingRating; }
    
    public float getRating() {
        return (qualityRating + sellerRating + shippingRating) / 3.0f;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSellerReply() { return sellerReply; }
    public void setSellerReply(String sellerReply) { this.sellerReply = sellerReply; }
    public long getReplyTimestamp() { return replyTimestamp; }
    public void setReplyTimestamp(long replyTimestamp) { this.replyTimestamp = replyTimestamp; }
}
