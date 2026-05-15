package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import com.google.gson.annotations.SerializedName;

public class CreateAuctionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("itemId")
    private long itemId;

    @SerializedName("sellerId")
    private long sellerId;

    @SerializedName("startTime")
    private String startTime; // ISO format: "2026-04-19T10:00:00"

    @SerializedName("endTime")
    private String endTime; // ISO format: "2026-04-19T12:00:00"

    @SerializedName("stepPrice")
    private BigDecimal stepPrice;

    public CreateAuctionDTO() {}

    public CreateAuctionDTO(long itemId, long sellerId, String startTime, String endTime, BigDecimal stepPrice) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.stepPrice = stepPrice;
    }

    // Getters
    public long getItemId() { return itemId; }
    public long getSellerId() { return sellerId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public BigDecimal getStepPrice() { return stepPrice; }

    // Setters
    public void setItemId(long itemId) { this.itemId = itemId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setStepPrice(BigDecimal stepPrice) { this.stepPrice = stepPrice; }

    @Override
    public String toString() {
        return "CreateAuctionDTO{" +
                "itemId=" + itemId +
                ", sellerId=" + sellerId +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", stepPrice=" + stepPrice +
                '}';
    }
}

