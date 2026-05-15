package com.server.service.auction.strategy;

import com.server.DAO.BidderRepository;
import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.server.model.Bidder;
import com.shared.dto.BidRequestDTO;
import java.math.BigDecimal;

/**
 * Implementation: Kiểm tra tính hợp lệ cơ bản của request và phiên đấu giá
 */

public class BasicBidValidation implements BidValidationStrategy {

    private final BidderRepository bidderRepo = new BidderRepository();

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        if (request.getBidAmount() == null) {
            throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "Giá đặt không được để trống");
        }

        if (auction == null) {
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND);
        }

        if (auction.getStatus() != Auction.AuctionStatus.RUNNING &&
                auction.getStatus() != Auction.AuctionStatus.OPEN) {
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_ACTIVE);
        }

        // --- FIX: KIỂM TRA SỐ DƯ TÀI KHOẢN ---
        Bidder bidder = bidderRepo.getBidderById(request.getBidderId());
        if (bidder == null) {
            throw new AuctionException(AuctionException.ErrorCode.USER_NOT_FOUND, "Không tìm thấy thông tin tài khoản");
        }

        // Kiểm tra xem ví chính có đủ tiền không (Lấy bidAmount vì nó là tổng tiền)
        BigDecimal currentWalletBalance = bidder.getWalletBalance();
        if (currentWalletBalance == null || currentWalletBalance.compareTo(request.getBidAmount()) < 0) {
            throw new AuctionException(AuctionException.ErrorCode.INSUFFICIENT_BALANCE,
                    "Số dư không đủ. Cần: " + request.getBidAmount() + ", Hiện có: " + currentWalletBalance);
        }
    }
}