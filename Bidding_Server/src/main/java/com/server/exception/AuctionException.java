package com.server.exception;

/**
 * Exception base cho các lỗi liên quan đến auction
 * Thay vì RuntimeException để có type safety
 */
public class AuctionException extends Exception {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        // Lỗi chung về phiên đấu giá
        AUCTION_NOT_FOUND("Phiên đấu giá không tồn tại hoặc chưa được tải."),
        AUCTION_NOT_ACTIVE("Phiên đấu giá không ở trạng thái hoạt động."),
        AUCTION_ALREADY_FINISHED("Phiên đấu giá đã kết thúc."),

        // Lỗi liên quan đến việc đặt giá
        INVALID_REQUEST("Yêu cầu không hợp lệ hoặc thiếu thông tin."),
        INVALID_BID_AMOUNT("Giá đặt không hợp lệ (ví dụ: thấp hơn giá khởi điểm)."),
        BID_AMOUNT_TOO_LOW("Giá đặt phải cao hơn giá hiện tại."),

        // Lỗi liên quan đến Auto-bid
        AUTO_BID_NOT_FOUND("Cấu hình auto-bid cho người dùng này không tồn tại."),
        INVALID_AUTO_BID_CONFIG("Cấu hình auto-bid không hợp lệ (ví dụ: giá tối đa không hợp lệ)."),

        // Lỗi hệ thống
        OPERATION_FAILED("Thao tác thất bại do lỗi hệ thống.");


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
