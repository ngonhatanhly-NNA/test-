package com.server.websocket;

import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.AuctionWinnerDTO;

public interface AuctionEventListener {
    void onAuctionUpdate(AuctionUpdateDTO update);
    void onAuctionCreated(AuctionDetailDTO detail);
    void onAuctionFinished(long auctionId);
    void onAuctionWon(AuctionWinnerDTO winnerData);
}
