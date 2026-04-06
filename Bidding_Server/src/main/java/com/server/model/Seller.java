package com.server.model;

public class Seller extends Bidder {
    private String shopName;
    private double rating;           // Điểm đánh giá
    private int totalReviews;        // Tổng số lượt đánh giá
    private String bankAccountNumber; //
    private boolean isVerified;

    // Kees thua bidder, nguoi cu, giu dc ca thong tin va walletBalance
    public Seller(){};
    public Seller(Bidder oldBidder, String shopName, String bankAccountNumber) {
        super(oldBidder.getId(), oldBidder.getUsername(), oldBidder.getPasswordHash(), oldBidder.getEmail(), oldBidder.getFullName(), oldBidder.getRole(), oldBidder.getPhoneNumber(),
                oldBidder.getAddress(), oldBidder.getStatus(), oldBidder.getCreditCardInfo());
        this.shopName = shopName;
        this.bankAccountNumber = bankAccountNumber; // HIện thị cho người ta xem stk, kia chỉ là nạp tiền
        this.rating = 0.0;           // Default là 0 sao, chưa có đánh giá
        this.totalReviews = 0;      
        this.isVerified = false;     // Default chưa đc xác minh
    }

    // Constructor DÀNH RIÊNG CHO DATABASE (Lấy toàn bộ data cũ) (Construct này thêm 1 tham số là walletBalance)
    public Seller(int id, String username, String passwordHash, String email, String fullName,
                  String phoneNumber, String address, String status, String role,
                  double walletBalance, String creditCardInfo,
                  String shopName, String bankAccountNumber, double rating, int totalReviews, boolean isVerified) {

        // Gọi thẳng lên Constructor DB của Bidder vừa tạo ở trên!
        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, role, walletBalance, creditCardInfo);

        this.shopName = shopName;
        this.bankAccountNumber = bankAccountNumber;
        this.rating = rating;
        this.totalReviews = totalReviews;
        this.isVerified = isVerified;
    }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber;}

    
	// Bảo mật
    public boolean isVerified() { return isVerified; }
    public void changeRole(){
        if (isVerified){
            setRole("SELLER");
        }
    }
    public double getRating() { return rating; }
    public int getTotalReviews() { return totalReviews; }

    // Khi có bidder đánh giá, system auto gọi
    public void addReviewScore(double score) {
        if (score >= 1.0 && score <= 5.0) {
            this.rating = ((this.rating * this.totalReviews) + score) / (this.totalReviews + 1);
            this.totalReviews++;
        } else {
            System.out.println("Lỗi: Điểm đánh giá phải từ 1 đến 5 sao.");
        }
    }
}