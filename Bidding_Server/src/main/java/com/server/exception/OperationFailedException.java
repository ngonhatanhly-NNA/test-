package com.server.exception;

/**
 * Exception được ném ra khi một thao tác nội bộ của hệ thống (ví dụ: ghi vào database, gọi dịch vụ ngoài)
 * thất bại một cách không mong muốn.
 * <p>
 * Lớp này giúp che giấu các chi tiết lỗi kỹ thuật phức tạp (như SQLException)
 * và đưa ra một thông báo lỗi chung, an toàn hơn cho phía client.
 * <p>
 * Thường dẫn đến mã lỗi HTTP 500 Internal Server Error.
 */
public class OperationFailedException extends AppException {

    /**
     * Constructor với một thông báo tùy chỉnh, mô tả thao tác đã thất bại.
     * @param message Mô tả về thao tác thất bại (ví dụ: "Không thể lưu người dùng vào database").
     */
    public OperationFailedException(String message) {
        super("OPERATION_FAILED", message, 500);
    }
}
