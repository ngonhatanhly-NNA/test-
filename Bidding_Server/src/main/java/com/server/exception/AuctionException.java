package com.server.exception;

/**
 * Exception base cho các lỗi liên quan đến auction
 * Thay vì RuntimeException để có type safety
 */
public class AuctionException extends Exception {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        AUCTION_NOT_FOUND("Phiên đấu giá không tồn tại"),
        AUCTION_NOT_ACTIVE("Phiên đấu giá không ở trạng thái hoạt động"),
        AUCTION_ALREADY_FINISHED("Phiên đấu giá đã kết thúc"),
        INVALID_BID_AMOUNT("Giá đặt không hợp lệ"),
        BID_AMOUNT_TOO_LOW("Giá đặt quá thấp"),
        INVALID_AUTO_BID_CONFIG("Cấu hình auto-bid không hợp lệ");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public AuctionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuctionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public AuctionException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

