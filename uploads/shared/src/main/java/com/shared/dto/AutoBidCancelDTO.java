package com.shared.dto;

import java.io.Serializable;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;


/**
 * DTO để hủy auto-bid
 */
public class AutoBidCancelDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("auctionId")
    private long auctionId;

    @SerializedName("bidderId")
    private long bidderId;

    public AutoBidCancelDTO() {}

    public AutoBidCancelDTO(long auctionId, long bidderId) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
    }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public long getBidderId() { return bidderId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoBidCancelDTO that = (AutoBidCancelDTO) o;
        return auctionId == that.auctionId && bidderId == that.bidderId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(auctionId, bidderId);
    }

    @Override
    public String toString() {
        return "AutoBidCancelDTO{" +
                "auctionId=" + auctionId +
                ", bidderId=" + bidderId +
                '}';
    }
}
