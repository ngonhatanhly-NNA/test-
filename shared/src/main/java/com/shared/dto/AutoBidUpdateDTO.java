package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * DTO để cập nhật giá tối đa auto-bid, handle client chỉnh chế độ auto-manual
 */
public class AutoBidUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("auctionId")
    private long auctionId;

    @SerializedName("bidderId")
    private long bidderId;

    @SerializedName("maxBidAmount")
    private BigDecimal maxBidAmount;

    @SerializedName("customStepPrice")
    private BigDecimal customStepPrice;

    public AutoBidUpdateDTO() {}

    public AutoBidUpdateDTO(long auctionId, long bidderId, BigDecimal maxBidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxBidAmount = maxBidAmount;
    }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public long getBidderId() { return bidderId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }

    public BigDecimal getMaxBidAmount() { return maxBidAmount; }
    public void setMaxBidAmount(BigDecimal maxBidAmount) { this.maxBidAmount = maxBidAmount; }

    public BigDecimal getCustomStepPrice() {
        return customStepPrice;
    }

    public void setCustomStepPrice(BigDecimal customStepPrice) {
        this.customStepPrice = customStepPrice;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoBidUpdateDTO that = (AutoBidUpdateDTO) o;
        return auctionId == that.auctionId && bidderId == that.bidderId && Objects.equals(maxBidAmount, that.maxBidAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, bidderId, maxBidAmount);
    }

    @Override
    public String toString() {
        return "AutoBidUpdateDTO{" +
                "auctionId=" + auctionId +
                ", bidderId=" + bidderId +
                ", maxBidAmount=" + maxBidAmount +
                '}';
    }
}
