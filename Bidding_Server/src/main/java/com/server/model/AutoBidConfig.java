package com.server.model;

// Thuộc phần hàm nâng cao AutoBidConfig
public class AutoBidConfig {
    private double maxBid;     // Giá người dùng chi
    private double increment;  // Bước gi nhảy

    public AutoBidConfig(double maxBid, double increment) {
        this.maxBid = maxBid;
        this.increment = increment;
    }

    // Getter, Setter
    public double getMaxBid(){return this.maxBid;}
    public double getIncrement(){return this.increment;}

    public void setMaxBid(double newBid){this.maxBid = newBid;}
    public void setIncrement(double newIncrement){this.increment = newIncrement;}
}
