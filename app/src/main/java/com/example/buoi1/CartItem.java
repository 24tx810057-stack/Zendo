package com.example.buoi1;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class CartItem implements Serializable {
    private String id;
    private String productId;
    private String productName;
    private double productPrice;
    private String productImageUrl;
    private int quantity;
    private String userEmail;
    private String warranty; // Lưu thông tin bảo hành tại thời điểm đặt hàng
    
    @Exclude
    private boolean isSelected = false;

    public CartItem() {
    }

    public CartItem(String id, String productId, String productName, double productPrice, String productImageUrl, int quantity, String userEmail) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.productImageUrl = productImageUrl;
        this.quantity = quantity;
        this.userEmail = userEmail;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getWarranty() { return warranty; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    @Exclude
    public boolean isSelected() { return isSelected; }
    @Exclude
    public void setSelected(boolean selected) { isSelected = selected; }
}
