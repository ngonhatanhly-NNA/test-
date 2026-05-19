package com.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.google.gson.annotations.SerializedName;

public class BidHistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("bidderId")
    private long bidderId;

    @SerializedName("bidderName")
    private String bidderName;

    @SerializedName("bidAmount")
    private BigDecimal bidAmount;

    @SerializedName("timestamp")
    private LocalDateTime timestamp;

    @SerializedName("isAutoBid")
    private boolean isAutoBid;

    public BidHistoryDTO() {}

    public BidHistoryDTO(long bidderId, String bidderName, BigDecimal bidAmount, LocalDateTime timestamp, boolean isAutoBid) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
        this.isAutoBid = isAutoBid;
    }

    // Getters
    public long getBidderId() { return bidderId; }
    public String getBidderName() { return bidderName; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAutoBid() { return isAutoBid; }

    // Setters
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setIsAutoBid(boolean isAutoBid) { this.isAutoBid = isAutoBid; }

    @Override
    public String toString() {
        return "BidHistoryDTO{" +
                "bidderId=" + bidderId +
                ", bidderName='" + bidderName + '\'' +
                ", bidAmount=" + bidAmount +
                ", timestamp=" + timestamp +
                ", isAutoBid=" + isAutoBid +
                '}';
    }
}

