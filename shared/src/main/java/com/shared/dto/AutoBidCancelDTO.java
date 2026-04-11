package com.shared.dto;

import java.io.Serializable;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;


/**
 * DTO để hủy auto-bid
 */
public class AutoBidCancelDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("bidderId")
    private long bidderId;

    public AutoBidCancelDTO() {}

    public AutoBidCancelDTO(long bidderId) {
        this.bidderId = bidderId;
    }

    public long getBidderId() { return bidderId; }
    public void setBidderId(long bidderId) { this.bidderId = bidderId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoBidCancelDTO that = (AutoBidCancelDTO) o;
        return bidderId == that.bidderId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bidderId);
    }

    @Override
    public String toString() {
        return "AutoBidCancelDTO{" +
                "bidderId=" + bidderId +
                '}';
    }
}
