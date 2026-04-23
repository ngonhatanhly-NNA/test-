package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

public class AuctionDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("auctionId")
    private long auctionId;

    @SerializedName("itemId")
    private long itemId;

    @SerializedName("itemName")
    private String itemName;

    @SerializedName("currentPrice")
    private BigDecimal currentPrice;

    @SerializedName("highestBidderName")
    private String highestBidderName;

    @SerializedName("remainingTime")
    private long remainingTime;

    @SerializedName("stepPrice")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionDetailDTO that = (AuctionDetailDTO) o;
        return auctionId == that.auctionId && itemId == that.itemId && remainingTime == that.remainingTime &&
                Objects.equals(itemName, that.itemName) && Objects.equals(currentPrice, that.currentPrice) &&
                Objects.equals(highestBidderName, that.highestBidderName) && Objects.equals(stepPrice, that.stepPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, itemId, itemName, currentPrice, highestBidderName, remainingTime, stepPrice);
    }

    @Override
    public String toString() {
        return "AuctionDetailDTO{" +
                "auctionId=" + auctionId +
                ", itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", currentPrice=" + currentPrice +
                ", highestBidderName='" + highestBidderName + '\'' +
                ", remainingTime=" + remainingTime +
                ", stepPrice=" + stepPrice +
                '}';
    }

}