package com.example.buoi1;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Order implements Serializable {
    private String id;
    private String userEmail;
    private List<CartItem> items;
    private double totalAmount; // Tổng cuối cùng khách phải trả
    private double subtotal;    // Tổng tiền hàng (chưa trừ voucher, chưa cộng ship)
    private double shippingFee; // Phí vận chuyển
    private double voucherDiscount; // Số tiền được giảm từ voucher
    private String paymentMethod;
    private String status; // Chờ xác nhận, Đang giao, Đã giao, Đã hủy
    private Date timestamp;
    private String address;
    private String phone;
    private String userName;
    private String note;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }

    public double getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(double voucherDiscount) { this.voucherDiscount = voucherDiscount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
