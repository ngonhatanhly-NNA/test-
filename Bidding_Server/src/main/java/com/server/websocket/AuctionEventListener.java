package com.server.websocket;
import com.shared.dto.AuctionUpdateDTO;
public interface AuctionEventListener {
    void onAuctionUpdate(AuctionUpdateDTO update);
}
