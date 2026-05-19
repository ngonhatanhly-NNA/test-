package com.client.util.ui.state;

public class StateFactory {
    public static AuctionUIState getButtonState(String status) {
        if ("ACTIVE".equals(status)) return new ActiveAuctionState();
        if ("FINISHED".equals(status)) return new FinishedAuctionState();
        return new NotStartedAuctionState(); // Mặc định
    }
}