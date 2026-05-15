package com.client.util.event;

public class AuctionFinishedEvent {
    public final long auctionId;

    public AuctionFinishedEvent(long auctionId) {
        this.auctionId = auctionId;
    }
}