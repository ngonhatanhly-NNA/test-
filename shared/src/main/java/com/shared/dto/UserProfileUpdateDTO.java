package com.shared.dto;

import java.math.BigDecimal;

public class UserProfileUpdateDTO {

    // --- Bảng users ---
    private int id; // Code Repo bạn đang dùng int cho ID
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String role; // "ADMIN", "SELLER", "BIDDER"
    private String status; // "ACTIVE", ...

    // --- Bảng bidders ---
    private BigDecimal walletBalance;
    private String creditCardInfo;

    // --- Bảng sellers ---
    private String shopName;
    private String bankAccountNumber;
    private Double rating;
    private Integer totalReviews;
    private Boolean isVerified;

    // --- Bảng admins ---
    private String roleLevel;
    private String lastLoginIp;

    public UserProfileUpdateDTO() {
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public String getCreditCardInfo() { return creditCardInfo; }
    public String getShopName() { return shopName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public Double getRating() { return rating; }
    public Integer getTotalReviews() { return totalReviews; }
    public Boolean getIsVerified() { return isVerified; }
    public String getRoleLevel() { return roleLevel; }
    public String getLastLoginIp() { return lastLoginIp; }

    // --- SETTERS ---
    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public void setCreditCardInfo(String creditCardInfo) { this.creditCardInfo = creditCardInfo; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public void setRating(Double rating) { this.rating = rating; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public void setRoleLevel(String roleLevel) { this.roleLevel = roleLevel; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
}