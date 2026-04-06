package com.server.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private long itemId;
    private long sellerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal stepPrice;
    private BigDecimal currentHighestBid;
    private Long winnerId;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory = new ArrayList<>();

    public enum AuctionStatus { OPEN, RUNNING, FINISHED, CANCELED }

    public Auction() {
        super(); // Gọi Entity constructor
    }

    // Override setters để auto update timestamp
    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
        updateTimestamp();
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
        updateTimestamp();
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        updateTimestamp();
    }

    // ... other getters/setters
    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }
    public long getSellerId() { return sellerId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BigDecimal getStepPrice() { return stepPrice; }
    public void setStepPrice(BigDecimal stepPrice) { this.stepPrice = stepPrice; }
    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }

    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
    public AuctionStatus getStatus() { return status; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
}