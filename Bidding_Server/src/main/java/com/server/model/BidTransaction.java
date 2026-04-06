package com.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private long auctionId;
    private long bidderId;
    private BigDecimal bidAmount;
    private LocalDateTime timestamp;
    private boolean isAutoBid;

    public BidTransaction(long auctionId, long bidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
        this.isAutoBid = false;
        updateTimestamp();
    }

    // Getters/Setters
    public long getAuctionId() { return auctionId; }
    public long getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAutoBid() { return isAutoBid; }
    public void setAutoBid(boolean autoBid) { isAutoBid = autoBid; }
}