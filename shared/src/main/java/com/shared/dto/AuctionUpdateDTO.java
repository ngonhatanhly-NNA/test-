package com.shared.dto;
//CHo Websocket broadcast
import java.math.BigDecimal;

public class AuctionUpdateDTO {
    private long auctionId;
    private BigDecimal currentPrice;
    private String highestBidderName;
    private long remainingTime;

    public AuctionUpdateDTO(long auctionId, BigDecimal currentPrice,
                            String highestBidderName, long remainingTime) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.highestBidderName = highestBidderName;
        this.remainingTime = remainingTime;
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getHighestBidderName() { return highestBidderName; }
    public long getRemainingTime() { return remainingTime; }
}