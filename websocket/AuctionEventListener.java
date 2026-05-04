package com.server.websocket;

import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;

public interface AuctionEventListener {
    void onAuctionUpdate(AuctionUpdateDTO update);

    default void onAuctionCreated(AuctionDetailDTO detail) {
    }

    default void onAuctionFinished(long auctionId) {
    }
}
