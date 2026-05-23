package com.server.exception;

/**
 * Exception base cho các lỗi liên quan đến auction
 * Thay vì RuntimeException để có type safety
 */
public class AuctionException extends Exception {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        // Lỗi chung về phiên đấu giá
        AUCTION_NOT_FOUND("Auction does not exist or has not been loaded."),
        AUCTION_NOT_ACTIVE("Auction is not currently active."),
        AUCTION_ALREADY_FINISHED("Auction has already ended."),

        // Lỗi liên quan đến việc đặt giá
        INVALID_REQUEST("Invalid request or missing information."),
        INVALID_BID_AMOUNT("Invalid bid amount (e.g., lower than starting price)."),
        BID_AMOUNT_TOO_LOW("Bid amount must be higher than the current price."),

        // Lỗi liên quan đến Auto-bid
        AUTO_BID_NOT_FOUND("Auto-bid configuration for this user does not exist."),
        INVALID_AUTO_BID_CONFIG("Invalid auto-bid configuration (e.g., invalid maximum price)."),

        // Lỗi tài chính và người dùng
        USER_NOT_FOUND("User account information not found."),
        INSUFFICIENT_BALANCE("Insufficient wallet balance to perform this transaction."),
        SELF_BID_NOT_ALLOWED("Sellers are not allowed to bid on their own auctions."),

        // Lỗi hệ thống
        OPERATION_FAILED("Operation failed due to a system error.");

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