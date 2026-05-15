package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;
import java.math.BigDecimal;

/**
 * Implementation: Kiểm tra giá tối thiểu (Minimum Increment)
 *  * Đối với hệ thống Delta Bidding: Số tiền gửi lên (Tiền cộng thêm) phải >= Bước giá quy định
 * bidAmount gửi lên là TỔNG GIÁ MUỐN MUA, không phải giá cộng thêm!
 */
public class MinimumIncrementValidation implements BidValidationStrategy {

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        BigDecimal desiredPrice = request.getBidAmount(); // Tổng tiền muốn mua
        BigDecimal currentPrice = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;
        BigDecimal stepPrice = auction.getStepPrice();

        // 1. Phải cao hơn giá hiện tại
        if (desiredPrice.compareTo(currentPrice) <= 0) {
            throw new AuctionException(
                    AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                    "Giá đặt (" + desiredPrice.toPlainString() + ") phải cao hơn giá hiện tại (" + currentPrice.toPlainString() + ")"
            );
        }

        // 2. Độ chênh lệch phải >= Bước giá
        BigDecimal difference = desiredPrice.subtract(currentPrice);
        if (stepPrice != null && stepPrice.compareTo(BigDecimal.ZERO) > 0) {
            if (difference.compareTo(stepPrice) < 0) {
                throw new AuctionException(
                        AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                        "Mức giá đặt chưa đủ. Bạn phải trả thêm ít nhất " + stepPrice.toPlainString() + " VNĐ"
                );
            }
        }
    }
}