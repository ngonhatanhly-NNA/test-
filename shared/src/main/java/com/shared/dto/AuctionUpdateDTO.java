package com.shared.dto;
//CHo Websocket broadcast
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

public class AuctionUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("auctionId")
    private long auctionId;

    @SerializedName("currentPrice")
    private BigDecimal currentPrice;

    @SerializedName("highestBidderName")
    private String highestBidderName;

    @SerializedName("remainingTime")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionUpdateDTO that = (AuctionUpdateDTO) o;
        return auctionId == that.auctionId && remainingTime == that.remainingTime &&
                Objects.equals(currentPrice, that.currentPrice) && Objects.equals(highestBidderName, that.highestBidderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, currentPrice, highestBidderName, remainingTime);
    }

    @Override
    public String toString() {
        return "AuctionUpdateDTO{" +
                "auctionId=" + auctionId +
                ", currentPrice=" + currentPrice +
                ", highestBidderName='" + highestBidderName + '\'' +
                ", remainingTime=" + remainingTime +
                '}';
    }
}