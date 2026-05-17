package com.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho một giao dịch ví của người dùng.
 * type = "DEPOSIT"  -> Nạp tiền vào ví
 * type = "DEDUCT"   -> Trừ tiền khi thắng đấu giá
 */
public class WalletTransactionDTO {

    private long id;
    private BigDecimal amount;       // Số tiền (luôn dương; dùng type để biết nạp/trừ)
    private String type;             // "DEPOSIT" hoặc "DEDUCT"
    private String description;      // Mô tả: "Nạp tiền qua thẻ..." hoặc "Thanh toán đấu giá: Audi"
    private LocalDateTime createdAt;

    public WalletTransactionDTO() {}

    public WalletTransactionDTO(long id, BigDecimal amount, String type,
                                String description, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    // --- Getters ---
    public long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Setters ---
    public void setId(long id) { this.id = id; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setType(String type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}