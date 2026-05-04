package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

public class BidRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("auctionId")
    private long auctionId;

    @SerializedName("bidderId")
    private long bidderId;

    @SerializedName("bidAmount")
    private BigDecimal bidAmount;

    @SerializedName("enableAutoBid")
    private boolean enableAutoBid;

    @SerializedName("maxAutoBidAmount")
    private BigDecimal maxAutoBidAmount;

    @SerializedName("expectedCurrentBid")
    private BigDecimal expectedCurrentBid;

    // THÊM MỚI: Bước giá tự chỉnh của người dùng
    @SerializedName("customStepPrice")
    private BigDecimal customStepPrice;

    // Constructors
    public BidRequestDTO() {}
    
    public BidRequestDTO(long auctionId, long bidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.enableAutoBid = false;
    }

    // Constructor riêng cho auto-bid (đã thêm tham số customStepPrice)
    public BidRequestDTO(long auctionId, long bidderId, BigDecimal bidAmount, boolean enableAutoBid, BigDecimal maxAutoBidAmount, BigDecimal customStepPrice) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.enableAutoBid = enableAutoBid;
        this.maxAutoBidAmount = maxAutoBidAmount;
        this.customStepPrice = customStepPrice;
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public long getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public BigDecimal getMaxAutoBidAmount() { return maxAutoBidAmount; }
    public boolean isEnableAutoBid() { return enableAutoBid; }
    public BigDecimal getExpectedCurrentBid() { return expectedCurrentBid; }
    public BigDecimal getCustomStepPrice() { return customStepPrice; } // Getter mới

    // Setters
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
    public void setEnableAutoBid(boolean enableAutoBid) { this.enableAutoBid = enableAutoBid; }
    public void setMaxAutoBidAmount(BigDecimal maxAutoBidAmount) { this.maxAutoBidAmount = maxAutoBidAmount; }
    public void setExpectedCurrentBid(BigDecimal expectedCurrentBid) { this.expectedCurrentBid = expectedCurrentBid; }
    public void setCustomStepPrice(BigDecimal customStepPrice) { this.customStepPrice = customStepPrice; } // Setter mới

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BidRequestDTO that = (BidRequestDTO) o;
        return auctionId == that.auctionId && 
               bidderId == that.bidderId && 
               enableAutoBid == that.enableAutoBid &&
               Objects.equals(bidAmount, that.bidAmount) && 
               Objects.equals(maxAutoBidAmount, that.maxAutoBidAmount) &&
               Objects.equals(customStepPrice, that.customStepPrice); // Thêm vào equals
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, bidderId, bidAmount, enableAutoBid, maxAutoBidAmount, customStepPrice); // Thêm vào hashCode
    }

    @Override
    public String toString() {
        return "BidRequestDTO{" +
                "auctionId=" + auctionId +
                ", bidderId=" + bidderId +
                ", bidAmount=" + bidAmount +
                ", enableAutoBid=" + enableAutoBid +
                ", maxAutoBidAmount=" + maxAutoBidAmount +
                ", customStepPrice=" + customStepPrice +
                '}';
    }
}