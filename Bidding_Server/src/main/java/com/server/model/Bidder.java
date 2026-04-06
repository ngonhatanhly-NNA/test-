package com.server.model;

public class Bidder extends User {
    private double walletBalance;
    private String creditCardInfo;

    public Bidder(){};

    // Thiết kế cho riêng Register
    
    public Bidder(String username, String passwordHash, String email, String fullname, String role){
        // Số 0 đầu tiên chính là ID! 
        // Khi truyền ID = 0 xuống Database, MySQL sẽ tự hiểu và AUTO_INCREMENT sinh ra ID mới.
        //  null là số điện thoại và địa chỉ (để trống chờ edit sau).
        // "ACTIVE" là trạng thái cấp cho user để họ login được luôn.
        super(0, username, passwordHash, email, fullname, null, null, "ACTIVE", "BIDDER");
        
        // Tiền ban đầu tất nhiên là 0 đồng. Thẻ tín dụng cũng tự động null.
        this.walletBalance = 0.0;
    }
    public Bidder(int id, String username, String passwordHash, String email, String fullName, String role,String phoneNumber, String address, String status, String creditCardInfo) {
        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, "BIDDER");
        this.walletBalance = 0.0; // Balance chỉ có đầu khi khởi tạo, sau phải nạp tiền, rút tiền
        this.creditCardInfo = creditCardInfo;
    }

    // Constructor DÀNH RIÊNG CHO DATABASE (Lấy toàn bộ data cũ) (Construct này thêm 1 tham số là walletBalance)
    public Bidder(int id, String username, String passwordHash, String email, String fullName,
                  String phoneNumber, String address, String status, String role,
                  double walletBalance, String creditCardInfo) {

        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, role);
        this.walletBalance = walletBalance; // Gán số dư thật từ DB
        this.creditCardInfo = creditCardInfo;
    }

    // Getter
    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) {this.walletBalance = walletBalance;}
    // Nạp tiền, dung setter ntn de tranh hacker 1 phat set tien am
    public void addFunds(double amount) {
        if (amount > 0) {
            this.walletBalance += amount;
        } else {
            System.out.println("Lỗi: Số tiền nạp phải lớn hơn 0.");
        }
    }

    // Thanh toán trừ tiền
    public boolean deductFunds(double amount) {
        if (amount > 0 && this.walletBalance >= amount) {
            this.walletBalance -= amount;
            return true; // Trừ tiền thành công
        }
        System.out.println("Lỗi: Số dư không đủ hoặc số tiền không hợp lệ.");
        return false; // Trừ tiền thất bại
    }

    // Getter and setter
    public String getCreditCardInfo() { return creditCardInfo; }
    public void setCreditCardInfo(String creditCardInfo) { this.creditCardInfo = creditCardInfo; }
}