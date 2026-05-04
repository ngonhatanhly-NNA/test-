package com.server.model;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bidder extends User {
    private static final Logger logger = LoggerFactory.getLogger(Bidder.class);
    private BigDecimal walletBalance;
    private String creditCardInfo;

    public Bidder() {}

    // Thiết kế cho riêng Register
    public Bidder(String username, String passwordHash, String email, String fullname) {
        // TRUYỀN TRỰC TIẾP ENUM: Status.ACTIVE và Role.BIDDER
        super(0L, username, passwordHash, email, fullname, null, null, Status.ACTIVE, Role.BIDDER);
        this.walletBalance = BigDecimal.ZERO;
    }

    public Bidder(long id, String username, String passwordHash, String email, String fullName, String phoneNumber, String address, Status status, String creditCardInfo) {
        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, Role.BIDDER);
        this.walletBalance = BigDecimal.ZERO;
        this.creditCardInfo = creditCardInfo;
    }

    // Constructor DÀNH RIÊNG CHO DATABASE (Đã đổi kiểu Status, Role)
    public Bidder(long id, String username, String passwordHash, String email, String fullName,
                  String phoneNumber, String address, Status status, Role role,
                  BigDecimal walletBalance, String creditCardInfo) {
        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, role);
        this.walletBalance = walletBalance;
        this.creditCardInfo = creditCardInfo;
    }

    // --- GETTERS & SETTERS ---
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }

    public String getCreditCardInfo() { return creditCardInfo; }
    public void setCreditCardInfo(String creditCardInfo) { this.creditCardInfo = creditCardInfo; }

    @Override
    public long getId() { return super.getId(); }

    // --- NGHIỆP VỤ ---
    public void addFunds(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.walletBalance = this.walletBalance.add(amount);
        } else {
            logger.error("Lỗi: Số tiền nạp phải lớn hơn 0.");
        }
    }

    public boolean deductFunds(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0 && this.walletBalance.compareTo(amount) >= 0) {
            this.walletBalance = this.walletBalance.subtract(amount);
            return true;
        }
        logger.error("Lỗi: Số dư không đủ hoặc số tiền không hợp lệ.");
        return false;
    }
}