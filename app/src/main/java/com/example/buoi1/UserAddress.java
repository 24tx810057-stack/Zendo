package com.example.buoi1;

import java.io.Serializable;

public class UserAddress implements Serializable {
    private String id;
    private String fullName;
    private String phone;
    private String provinceCity;
    private String district;
    private String ward;
    private String detailAddress;
    private boolean isDefault;
    private boolean isPickupAddress; // Mới: Địa chỉ lấy hàng
    private boolean isReturnAddress; // Mới: Địa chỉ trả hàng
    private String type;          // "Nhà riêng" hoặc "Văn phòng"

    public UserAddress() {}

    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProvinceCity() { return provinceCity; }
    public void setProvinceCity(String provinceCity) { this.provinceCity = provinceCity; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public boolean isPickupAddress() { return isPickupAddress; }
    public void setPickupAddress(boolean pickupAddress) { isPickupAddress = pickupAddress; }
    public boolean isReturnAddress() { return isReturnAddress; }
    public void setReturnAddress(boolean returnAddress) { isReturnAddress = returnAddress; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFullAddress() {
        return detailAddress + ", " + ward + ", " + district + ", " + provinceCity;
    }
}
