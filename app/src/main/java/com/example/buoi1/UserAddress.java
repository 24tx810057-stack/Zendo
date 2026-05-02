package com.example.buoi1;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
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
    private String type;
    private String userEmail;

    public UserAddress() {}

    @Exclude
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

    @PropertyName("default")
    public boolean isDefault() { return isDefault; }
    @PropertyName("default")
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getFullAddress() {
        return detailAddress + ", " + ward + ", " + district + ", " + provinceCity;
    }
}
