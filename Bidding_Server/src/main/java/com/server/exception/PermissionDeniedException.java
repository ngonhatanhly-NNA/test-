package com.server.exception;

/**
 * Exception được ném ra khi một người dùng đã được xác thực (đã đăng nhập)
 * nhưng không có đủ quyền để thực hiện một hành động hoặc truy cập một tài nguyên cụ thể.
 * <p>
 * Ví dụ: Một người dùng vai trò 'BIDDER' cố gắng sử dụng chức năng chỉ dành cho 'ADMIN'.
 * <p>
 * Thường dẫn đến mã lỗi HTTP 403 Forbidden.
 */
public class PermissionDeniedException extends AppException {

    /**
     * Constructor với một thông báo tùy chỉnh.
     * @param message Mô tả chi tiết về hành động bị từ chối.
     */
    public PermissionDeniedException(String message) {
        super("PERMISSION_DENIED", message, 403);
    }

    /**
     * Constructor mặc định với thông báo chung.
     */
    public PermissionDeniedException() {
        this("Bạn không có quyền thực hiện hành động này.");
    }
}
