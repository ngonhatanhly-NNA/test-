package com.server.model;

import java.math.BigDecimal;

public class Seller extends Bidder {
    private String shopName;
    private double rating;
    private int totalReviews;
    private String bankAccountNumber;
    private boolean isVerified;

    public Seller() {}

    // Khởi tạo Seller từ một Bidder cũ khi được nâng cấp
    public Seller(Bidder oldBidder, String shopName, String bankAccountNumber) {
        super(oldBidder.getId(), oldBidder.getUsername(), oldBidder.getPasswordHash(), oldBidder.getEmail(),
                oldBidder.getFullName(), oldBidder.getPhoneNumber(), oldBidder.getAddress(),
                oldBidder.getStatus(), oldBidder.getCreditCardInfo());

        this.shopName = shopName;
        this.bankAccountNumber = bankAccountNumber;
        this.rating = 0.0;
        this.totalReviews = 0;
        this.isVerified = false;

        // TRUYỀN TRỰC TIẾP ENUM ROLE
        this.setRole(Role.SELLER);
        this.setWalletBalance(oldBidder.getWalletBalance());
    }

    // Constructor DÀNH RIÊNG CHO DATABASE (Đã đổi kiểu Status, Role)
    public Seller(long id, String username, String passwordHash, String email, String fullName,
                  String phoneNumber, String address, Status status, Role role,
                  BigDecimal walletBalance, String creditCardInfo,
                  String shopName, String bankAccountNumber, double rating, int totalReviews, boolean isVerified) {

        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, role, walletBalance, creditCardInfo);
        this.shopName = shopName;
        this.bankAccountNumber = bankAccountNumber;
        this.rating = rating;
        this.totalReviews = totalReviews;
        this.isVerified = isVerified;
    }

    // --- GETTERS & SETTERS ---
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber;}

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }

    public double getRating() { return rating; }
    public int getTotalReviews() { return totalReviews; }

    // --- NGHIỆP VỤ ---
    public void addReviewScore(double score) {
        if (score >= 1.0 && score <= 5.0) {
            this.rating = ((this.rating * this.totalReviews) + score) / (this.totalReviews + 1);
            this.totalReviews++;
        } else {
            System.out.println("Lỗi: Điểm đánh giá phải từ 1 đến 5 sao.");
        }
    }
}