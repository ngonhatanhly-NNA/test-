package com.server.model;

import java.math.BigDecimal;

/**
 * Theo dõi thông tin auto-bid của từng người dùng cho từng phiên đấu giá
 * Lưu trữ giá tối đa mà người dùng muốn tự động đặt
 */
public class AutoBidTracker {
    private long auctionId;
    private long bidderId;
    private BigDecimal maxBidAmount; // Giá tối đa sẵn sàng trả
    private boolean active; // Trạng thái hoạt động
    private BigDecimal customStepPrice; //

    public AutoBidTracker() {}

    public AutoBidTracker(long auctionId, long bidderId, BigDecimal maxBidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxBidAmount = maxBidAmount;
        this.active = true;
    }

    // Getters and Setters
    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public long getBidderId() { return bidderId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }

    public BigDecimal getMaxBidAmount() { return maxBidAmount; }
    public void setMaxBidAmount(BigDecimal maxBidAmount) { this.maxBidAmount = maxBidAmount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getCustomStepPrice() {
        return customStepPrice;
    }

    public void setCustomStepPrice(BigDecimal customStepPrice) {
        this.customStepPrice = customStepPrice;
    }
    
    @Override
    public String toString() {
        return String.format("AutoBidTracker{auctionId=%d, bidderId=%d, maxBid=%s, active=%b}",
                auctionId, bidderId, maxBidAmount, active);
    }
}

