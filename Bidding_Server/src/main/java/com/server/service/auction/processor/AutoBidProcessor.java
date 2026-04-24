package com.server.service.auction.processor;

import com.server.model.Auction;
import com.server.model.AutoBidTracker;
import com.server.model.BidTransaction;
import com.server.DAO.AutoBidRepository;
import com.server.service.auction.logging.AuctionLogger;
import com.shared.dto.BidRequestDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;

/**
 * Xử lý tự động bid khi có bid mới hoặc khi phiên đấu giá mở cửa
 * - Lấy danh sách auto bid active cho phiên đấu giá
 * - Kiểm tra điều kiện và cập nhật phiên đấu giá nếu có auto bid hợp lệ
 * - Đẩy giao dịch auto bid vào queue để xử lý bất đồng bộ sau này
 */
public class AutoBidProcessor{
    private final AutoBidRepository autoBidRepository;

    public AutoBidProcessor(AutoBidRepository autoBidRepository) {
        this.autoBidRepository = autoBidRepository;
    }

    public void process( Auction auction, Queue<BidTransaction> bidQueue) {
        List<AutoBidTracker> activeBids = autoBidRepository.findAllActiveByAuction(auction.getId());

        for (AutoBidTracker autoBid : activeBids) {
            if (autoBid.getBidderId() == auction.getWinnerId()) continue;

            BigDecimal currentBid = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;
            BigDecimal minBid = currentBid.add(auction.getStepPrice());

            if (autoBid.getMaxBidAmount().compareTo(minBid) >= 0) {
                BigDecimal autoBidAmount = minBid.min(autoBid.getMaxBidAmount());

                // Cập nhật trạng thái phiên
                auction.setCurrentHighestBid(autoBidAmount);
                auction.setWinnerId(autoBid.getBidderId());

                // Đẩy vào Queue
                BidTransaction autoBidTx = new BidTransaction(auction.getId(), autoBid.getBidderId(), autoBidAmount);
                autoBidTx.setAutoBid(true);
                bidQueue.offer(autoBidTx);

                AuctionLogger.logBidPlaced(auction.getId(), autoBid.getBidderId(), autoBidAmount.toString(), true);
            } else {
                autoBidRepository.deactivate(auction.getId(), autoBid.getBidderId());
                AuctionLogger.logAutoBidDisabled(auction.getId(), autoBid.getBidderId(), "Hết ngân sách");
            }
        }
    }
}