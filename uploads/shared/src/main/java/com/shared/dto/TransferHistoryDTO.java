package com.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferHistoryDTO {
    private String buyerName;
    private BigDecimal amount;
    private LocalDateTime transferTime;
    private String itemName; // Tên sản phẩm đấu giá

    public TransferHistoryDTO(String buyerName, BigDecimal amount, LocalDateTime transferTime, String itemName) {
        this.buyerName = buyerName;
        this.amount = amount;
        this.transferTime = transferTime;
        this.itemName = itemName;
    }

    // Getters
    public String getBuyerName() {
        return buyerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTransferTime() {
        return transferTime;
    }

    public String getItemName() {
        return itemName;
    }

    // Setters (nếu cần, nhưng thường DTO chỉ cần getters)
    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setTransferTime(LocalDateTime transferTime) {
        this.transferTime = transferTime;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
