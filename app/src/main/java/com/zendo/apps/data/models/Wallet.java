package com.zendo.apps.data.models;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class Wallet implements Serializable {
    private String userEmail;
    private double balance;
    private String pinCode;
    private boolean isLinkedBank;
    private boolean isLinkedMoMo;
    
    // Detailed Bank Info
    private String bankName;
    private String accountNo;
    private String accountName;
    
    // Detailed MoMo Info
    private String momoPhone;

    public Wallet() {
    }

    public Wallet(String userEmail) {
        this.userEmail = userEmail;
        this.balance = 0;
        this.pinCode = "";
        this.isLinkedBank = false;
        this.isLinkedMoMo = false;
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    @PropertyName("isLinkedBank")
    public boolean isLinkedBank() { return isLinkedBank; }
    @PropertyName("isLinkedBank")
    public void setLinkedBank(boolean linkedBank) { isLinkedBank = linkedBank; }

    @PropertyName("isLinkedMoMo")
    public boolean isLinkedMoMo() { return isLinkedMoMo; }
    @PropertyName("isLinkedMoMo")
    public void setLinkedMoMo(boolean linkedMoMo) { isLinkedMoMo = linkedMoMo; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getMomoPhone() { return momoPhone; }
    public void setMomoPhone(String momoPhone) { this.momoPhone = momoPhone; }
}
