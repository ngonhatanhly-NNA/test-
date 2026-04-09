package com.shared.dto;

/**
 * DTO để hủy auto-bid
 */
public class AutoBidCancelDTO {
    private long bidderId;

    public AutoBidCancelDTO() {}

    public AutoBidCancelDTO(long bidderId) {
        this.bidderId = bidderId;
    }

    public long getBidderId() { return bidderId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }
}

