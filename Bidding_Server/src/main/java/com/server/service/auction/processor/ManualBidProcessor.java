package com.server.service.auction.processor;

import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.shared.dto.BidRequestDTO;

import java.math.BigDecimal;
import java.util.Queue;

/**
 * Implementation: Xử lý bid thủ công
 * @param request: Thông tin bid từ client
 * @param auction: Phiên đấu giá hiện tại
 * @param bidQueue: Queue để lưu giao dịch bid, xử lý bất đồng bộ sau này
 */
public class ManualBidProcessor implements BidProcessor {

    @Override
    public void process(BidRequestDTO request, Auction auction, Queue<BidTransaction> bidQueue) {
        // Cập nhật trạng thái OPEN -> RUNNING nếu cần
        if (auction.getStatus() == Auction.AuctionStatus.OPEN) {
            auction.setStatus(Auction.AuctionStatus.RUNNING);
        }

        BigDecimal currentBid = auction.getCurrentHighestBid() != null ? 
                                auction.getCurrentHighestBid() : BigDecimal.ZERO;
        // Cập nhật phiên đấu giá
        BigDecimal newTotalBid = currentBid.add(request.getBidAmount());
        auction.setCurrentHighestBid(newTotalBid);
        auction.setWinnerId(request.getBidderId());

        // Lưu vào queue để xử lý bất đồng bộ
        BidTransaction transaction = new BidTransaction(
            request.getAuctionId(),
            request.getBidderId(),
            newTotalBid
        );
        transaction.setAutoBid(false);
        bidQueue.offer(transaction);
    }
}

