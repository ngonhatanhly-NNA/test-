package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * DTO để cập nhật giá tối đa auto-bid, hanlde client chỉnh chế đọ auto-manual
 */
public class AutoBidUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("bidderId")
    private long bidderId;

    @SerializedName("maxBidAmount")
    private BigDecimal maxBidAmount;

    public AutoBidUpdateDTO() {}

    public AutoBidUpdateDTO(long bidderId, BigDecimal maxBidAmount) {
        this.bidderId = bidderId;
        this.maxBidAmount = maxBidAmount;
    }

    public long getBidderId() { return bidderId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }

    public BigDecimal getMaxBidAmount() { return maxBidAmount; }
    public void setMaxBidAmount(BigDecimal maxBidAmount) { this.maxBidAmount = maxBidAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoBidUpdateDTO that = (AutoBidUpdateDTO) o;
        return bidderId == that.bidderId && Objects.equals(maxBidAmount, that.maxBidAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bidderId, maxBidAmount);
    }

    @Override
    public String toString() {
        return "AutoBidUpdateDTO{" +
                "bidderId=" + bidderId +
                ", maxBidAmount=" + maxBidAmount +
                '}';
    }
}
