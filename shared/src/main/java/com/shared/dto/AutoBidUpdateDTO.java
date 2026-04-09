package com.shared.dto;

import java.math.BigDecimal;

/**
 * DTO để cập nhật giá tối đa auto-bid
 */
public class AutoBidUpdateDTO {
    private long bidderId;
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
}

