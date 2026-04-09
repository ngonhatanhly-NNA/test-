package com.server.service.auction.processor;

import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.shared.dto.BidRequestDTO;
import java.util.Queue;

/**
 * Implementation: Xử lý bid thủ công
 */
public class ManualBidProcessor implements BidProcessor {

    @Override
    public void process(BidRequestDTO request, Auction auction, Queue<BidTransaction> bidQueue) {
        // Cập nhật trạng thái OPEN -> RUNNING nếu cần
        if (auction.getStatus() == Auction.AuctionStatus.OPEN) {
            auction.setStatus(Auction.AuctionStatus.RUNNING);
        }

        // Cập nhật phiên đấu giá
        auction.setCurrentHighestBid(request.getBidAmount());
        auction.setWinnerId(request.getBidderId());

        // Lưu vào queue để xử lý bất đồng bộ
        BidTransaction transaction = new BidTransaction(
            request.getAuctionId(),
            request.getBidderId(),
            request.getBidAmount()
        );
        transaction.setAutoBid(false);
        bidQueue.offer(transaction);
    }
}

