package com.shared.dto;

import java.math.BigDecimal;

public class BidRequestDTO {
    private long auctionId;
    private long bidderId;
    private BigDecimal bidAmount;

    // Constructors, getters, setters
    public BidRequestDTO() {}
    public BidRequestDTO(long auctionId, long bidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }

    public long getAuctionId() { return auctionId; }
    public long getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
}