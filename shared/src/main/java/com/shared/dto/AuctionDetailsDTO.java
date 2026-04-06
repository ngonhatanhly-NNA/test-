package com.shared.dto;
import com.server.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionDetailsDTO {
    private long auctionId;
    private long itemId;
    private String itemName; // Lấy từ ItemService
    private BigDecimal currentPrice;
    private String highestBidderName;
    private long remainingTime;
    private BigDecimal stepPrice;
    private LocalDateTime endTime;

    public AuctionDetailsDTO(Auction auction) {
        this.auctionId = auction.getId();
        this.itemId = auction.getItemId();
        this.itemName = "Item #" + auction.getItemId(); // Gọi ItemService.getName()
        this.currentPrice = auction.getCurrentHighestBid();
        this.highestBidderName = auction.getWinnerId() != null ?
                "User_" + auction.getWinnerId() : "No bids";
        this.remainingTime = java.time.Duration.between(
                java.time.LocalDateTime.now(), auction.getEndTime()).toMillis();
        this.stepPrice = auction.getStepPrice();
        this.endTime = auction.getEndTime();
    }

    // Getters...
    public long getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public long getRemainingTime() { return remainingTime; }
    public BigDecimal getStepPrice() { return stepPrice; }
}