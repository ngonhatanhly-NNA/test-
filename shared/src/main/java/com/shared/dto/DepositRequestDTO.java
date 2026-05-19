package com.shared.dto;

import java.math.BigDecimal;

/**
 * DTO dùng để gửi request nạp tiền từ Client đến Server
 */
public class DepositRequestDTO {
    private long bidderId;
    private BigDecimal depositAmount;
    private String paymentMethod; // "CREDIT_CARD", "BANK_TRANSFER", etc.
    private String description; // Mô tả giao dịch (tùy chọn)

    // Constructor không tham số
    public DepositRequestDTO() {}

    // Constructor đầy đủ tham số
    public DepositRequestDTO(long bidderId, BigDecimal depositAmount, String paymentMethod, String description) {
        this.bidderId = bidderId;
        this.depositAmount = depositAmount;
        this.paymentMethod = paymentMethod;
        this.description = description;
    }

    // Constructor đơn giản (chỉ ID và số tiền)
    public DepositRequestDTO(long bidderId, BigDecimal depositAmount) {
        this.bidderId = bidderId;
        this.depositAmount = depositAmount;
        this.paymentMethod = "CREDIT_CARD";
        this.description = "Nạp tiền vào ví";
    }

    // Getters and Setters
    public long getBidderId() {
        return bidderId;
    }

    public void setBidderId(long bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

