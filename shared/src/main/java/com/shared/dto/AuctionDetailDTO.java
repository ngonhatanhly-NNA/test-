package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
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
    
    @SerializedName("itemType")
    private String itemType; // "VEHICLE", "ART", "ELECTRONICS"

    @SerializedName("itemSpecifics")
    private Map<String, String> itemSpecifics;

    public AuctionDetailDTO() {} // Bắt buộc phải có hàm rỗng cho Gson/Javalin

    public AuctionDetailDTO(long auctionId, long itemId, String itemName, BigDecimal currentPrice,
                             String highestBidderName, long remainingTime, BigDecimal stepPrice,
                             String itemType, Map<String, String> itemSpecifics) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.highestBidderName = highestBidderName;
        this.remainingTime = remainingTime;
        this.stepPrice = stepPrice;
        this.itemType = itemType;
        this.itemSpecifics = itemSpecifics;
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getHighestBidderName() { return highestBidderName; }
    public long getRemainingTime() { return remainingTime; }
    public BigDecimal getStepPrice() { return stepPrice; }
    public String getItemType() { return itemType; }
    public Map<String, String> getItemSpecifics() { return itemSpecifics; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionDetailDTO that = (AuctionDetailDTO) o;
        return auctionId == that.auctionId && itemId == that.itemId && remainingTime == that.remainingTime &&
                Objects.equals(itemName, that.itemName) && Objects.equals(currentPrice, that.currentPrice) &&
                Objects.equals(highestBidderName, that.highestBidderName) && Objects.equals(stepPrice, that.stepPrice) &&
                Objects.equals(itemType, that.itemType) && Objects.equals(itemSpecifics, that.itemSpecifics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, itemId, itemName, currentPrice, highestBidderName, remainingTime, stepPrice, itemType, itemSpecifics);
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
                ", itemType='" + itemType + '\'' +
                ", itemSpecifics=" + itemSpecifics +
                '}';
    }

}