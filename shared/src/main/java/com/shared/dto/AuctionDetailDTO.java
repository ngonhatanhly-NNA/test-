package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
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

    @SerializedName("itemDescription")
    private String itemDescription;

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

    @SerializedName("itemImageUrls")
    private List<String> itemImageUrls;

    @SerializedName("sellerId")
    private long sellerId;

    @SerializedName("sellerName")
    private String sellerName;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    public AuctionDetailDTO() {} // Bắt buộc phải có hàm rỗng cho Gson/Javalin

    public AuctionDetailDTO(long auctionId, long itemId, String itemName, String itemDescription,
                             BigDecimal currentPrice, String highestBidderName, long remainingTime,
                             BigDecimal stepPrice, String itemType, Map<String, String> itemSpecifics,
                             List<String> itemImageUrls, long sellerId,String sellerName, String startTime, String endTime) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.currentPrice = currentPrice;
        this.highestBidderName = highestBidderName;
        this.remainingTime = remainingTime;
        this.stepPrice = stepPrice;
        this.itemType = itemType;
        this.itemSpecifics = itemSpecifics;
        this.itemImageUrls = itemImageUrls;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getItemDescription() { return itemDescription; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getHighestBidderName() { return highestBidderName; }
    public long getRemainingTime() { return remainingTime; }
    public BigDecimal getStepPrice() { return stepPrice; }
    public String getItemType() { return itemType; }
    public Map<String, String> getItemSpecifics() { return itemSpecifics; }
    public List<String> getItemImageUrls() { return itemImageUrls; }
    public long getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionDetailDTO that = (AuctionDetailDTO) o;
        return auctionId == that.auctionId && itemId == that.itemId && remainingTime == that.remainingTime &&
                Objects.equals(itemName, that.itemName) && Objects.equals(itemDescription, that.itemDescription) &&
                Objects.equals(currentPrice, that.currentPrice) &&
                Objects.equals(highestBidderName, that.highestBidderName) && Objects.equals(stepPrice, that.stepPrice) &&
                Objects.equals(itemType, that.itemType) && Objects.equals(itemSpecifics, that.itemSpecifics) &&
                Objects.equals(itemImageUrls, that.itemImageUrls) && sellerId == that.sellerId &&
                Objects.equals(sellerName, that.sellerName) && Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, itemId, itemName, itemDescription, currentPrice, highestBidderName,
                remainingTime, stepPrice, itemType, itemSpecifics, itemImageUrls, sellerId, sellerName, startTime, endTime);
    }

    @Override
    public String toString() {
        return "AuctionDetailDTO{" +
                "auctionId=" + auctionId +
                ", itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", itemDescription='" + itemDescription + '\'' +
                ", currentPrice=" + currentPrice +
                ", highestBidderName='" + highestBidderName + '\'' +
                ", remainingTime=" + remainingTime +
                ", stepPrice=" + stepPrice +
                ", itemType='" + itemType + '\'' +
                ", itemSpecifics=" + itemSpecifics +
                ", itemImageUrls=" + itemImageUrls +
                ", sellerId=" + sellerId +
                ", sellerName='" + sellerName + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }

}