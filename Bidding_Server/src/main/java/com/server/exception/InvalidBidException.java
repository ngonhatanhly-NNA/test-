package com.server.exception;

import java.math.BigDecimal;

/**
 * Exception được ném ra khi một người dùng thực hiện một hành động đấu giá không hợp lệ.
 * Thường dẫn đến mã lỗi HTTP 400 Bad Request.
 */
public class InvalidBidException extends AppException {

    /**
     * Enum định nghĩa các lý do cụ thể tại sao một mức giá đấu không hợp lệ.
     * Giúp cho việc xử lý lỗi ở tầng trên trở nên rõ ràng và tường minh.
     */
    public enum BidError {
        BID_TOO_LOW("Giá đưa ra phải cao hơn giá cao nhất hiện tại."),
        BID_LOWER_THAN_START_PRICE("Giá đưa ra không được thấp hơn giá khởi điểm."),
        AUCTION_ENDED("Phiên đấu giá đã kết thúc."),
        INSUFFICIENT_FUNDS("Bạn không đủ tiền trong ví để thực hiện hành động này."),
        OWNER_CANNOT_BID("Chủ sở hữu sản phẩm không thể tham gia đấu giá."),
        INVALID_AMOUNT("Số tiền đưa ra không hợp lệ.");

        private final String message;

        BidError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public InvalidBidException(BidError error) {
        super(error.name(), error.getMessage(), 400); // HTTP 400 Bad Request
    }

    public InvalidBidException(BidError error, BigDecimal currentHighestBid) {
        super(error.name(), error.getMessage() + " (Giá hiện tại: " + currentHighestBid.toString() + ")", 400);
    }
}
