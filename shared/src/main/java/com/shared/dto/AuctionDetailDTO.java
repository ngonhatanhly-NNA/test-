package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionDetailDTO implements Serializable {
    private long auctionId;
    private long itemId;
    private String itemName;
    private BigDecimal currentPrice;
    private String highestBidderName;
    private long remainingTime;
    private BigDecimal stepPrice;
    // Bỏ qua biến endTime hoặc nếu cần thì dùng String/long để tránh lỗi parse JSON

    public AuctionDetailDTO() {} // Bắt buộc phải có hàm rỗng cho Gson/Javalin

    public AuctionDetailDTO(long auctionId, long itemId, String itemName, BigDecimal currentPrice,
                             String highestBidderName, long remainingTime, BigDecimal stepPrice) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.highestBidderName = highestBidderName;
        this.remainingTime = remainingTime;
        this.stepPrice = stepPrice;
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getHighestBidderName() { return highestBidderName; }
    public long getRemainingTime() { return remainingTime; }
    public BigDecimal getStepPrice() { return stepPrice; }
}