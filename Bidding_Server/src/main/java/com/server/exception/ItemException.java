package com.server.exception;

/**
 * Exception base cho các lỗi liên quan đến items
 */
public class ItemException extends AppException {

    public enum ErrorCode {
        ITEM_NOT_FOUND("ITEM_001", "Item not found."),
        ITEM_SAVE_FAILED("ITEM_002", "Failed to save item to the database."),
        ITEM_FETCH_FAILED("ITEM_003", "Failed to retrieve item list."),
        INVALID_ITEM_DATA("ITEM_004", "Invalid item data."),
        FACTORY_CREATE_FAILED("ITEM_005", "Failed to create item in Factory.");

        private final String code;
        private final String message;

        ErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    // Constructor nhận vào Enum ErrorCode của Item
    public ItemException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    // Constructor có thêm chi tiết lỗi (detail)
    public ItemException(ErrorCode errorCode, String detail) {
        super(errorCode.getCode(), errorCode.getMessage() + ": " + detail);
    }
}