package com.shared.dto;

import java.math.BigDecimal;

public class BidRequestDTO {
    private long auctionId;
    private long bidderId;
    private BigDecimal bidAmount;
    private boolean enableAutoBid;
    private BigDecimal maxAutoBidAmount;

    // Constructors, getters, setters
    public BidRequestDTO() {}
    public BidRequestDTO(long auctionId, long bidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.enableAutoBid = false;
    }

    // Constructor riêng cho ato-bid
    public BidRequestDTO(long auctionId, long bidderId, BigDecimal bidAmount, boolean enableAutoBid, BigDecimal maxAutoBidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.enableAutoBid = enableAutoBid;
        this.maxAutoBidAmount = maxAutoBidAmount;
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public long getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public BigDecimal getMaxAutoBidAmount() { return maxAutoBidAmount; }
    // Getter cho boolean thường dùng tiền tố "is"
    public boolean isEnableAutoBid() { return enableAutoBid; }

    // Setters (Nếu cần thiết cho việc deserialize JSON)
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
    public void setEnableAutoBid(boolean enableAutoBid) { this.enableAutoBid = enableAutoBid; }
    public void setMaxAutoBidAmount(BigDecimal maxAutoBidAmount) { this.maxAutoBidAmount = maxAutoBidAmount; }
}