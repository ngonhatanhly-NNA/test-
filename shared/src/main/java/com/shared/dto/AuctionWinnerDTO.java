package com.shared.dto;

import java.math.BigDecimal;

public class AuctionWinnerDTO {
    private long auctionId;
    private String itemName;
    private long winnerId;
    private String winnerName;
    private BigDecimal winningPrice;

    public AuctionWinnerDTO(long auctionId, String itemName, long winnerId, String winnerName, BigDecimal winningPrice) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.winnerId = winnerId;
        this.winnerName = winnerName;
        this.winningPrice = winningPrice;
    }
    
    public long getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }
    public long getWinnerId() { return winnerId; }
    public String getWinnerName() { return winnerName; }
    public BigDecimal getWinningPrice() { return winningPrice; }
}
