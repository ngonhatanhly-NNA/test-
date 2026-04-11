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

    // Constructors, getters, setters
    public BidRequestDTO() {}
    public BidRequestDTO(long auctionId, long bidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.enableAutoBid = false;
    }

    // Constructor riêng cho ato-bid
    public BidRequestDTO(long auctionId, long bidderId, BigDecimal bidAmount, boolean enableAutoBid, BigDecimal maxAutoBidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.enableAutoBid = enableAutoBid;
        this.maxAutoBidAmount = maxAutoBidAmount;
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public long getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public BigDecimal getMaxAutoBidAmount() { return maxAutoBidAmount; }
    // Getter cho boolean thường dùng tiền tố "is"
    public boolean isEnableAutoBid() { return enableAutoBid; }

    // Setters (Nếu cần thiết cho việc deserialize JSON)
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
    public void setEnableAutoBid(boolean enableAutoBid) { this.enableAutoBid = enableAutoBid; }
    public void setMaxAutoBidAmount(BigDecimal maxAutoBidAmount) { this.maxAutoBidAmount = maxAutoBidAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BidRequestDTO that = (BidRequestDTO) o;
        return auctionId == that.auctionId && bidderId == that.bidderId && enableAutoBid == that.enableAutoBid &&
                Objects.equals(bidAmount, that.bidAmount) && Objects.equals(maxAutoBidAmount, that.maxAutoBidAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, bidderId, bidAmount, enableAutoBid, maxAutoBidAmount);
    }

    @Override
    public String toString() {
        return "BidRequestDTO{" +
                "auctionId=" + auctionId +
                ", bidderId=" + bidderId +
                ", bidAmount=" + bidAmount +
                ", enableAutoBid=" + enableAutoBid +
                ", maxAutoBidAmount=" + maxAutoBidAmount +
                '}';
    }
}