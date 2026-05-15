package com.server.exception;

/**
 * Dữ liệu đăng ký / đăng nhập không hợp lệ (thiếu trường, rỗng, v.v.)
 * Kế thừa từ AppException để có thể được xử lý bởi GlobalExceptionHandler.
 */
public class AuthValidationException extends AppException {

    /**
     * Enum nội bộ để định nghĩa các mã lỗi validation một cách rõ ràng.
     */
    public enum ErrorCode {
        INVALID_REQUEST_DATA("Dữ liệu không hợp lệ."),
        USERNAME_BLANK("Vui lòng nhập tài khoản."),
        PASSWORD_BLANK("Vui lòng nhập mật khẩu."),
        EMAIL_BLANK("Vui lòng nhập email."),
        FULLNAME_BLANK("Vui lòng nhập họ và tên.");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Constructor mới, nhận vào một ErrorCode enum.
     * Đây là cách dùng được khuyến khích cho code mới.
     * @param errorCode Mã lỗi từ enum ErrorCode.
     */
    public AuthValidationException(ErrorCode errorCode) {
        super(errorCode.name(), errorCode.getMessage(), 400);
    }

    /**
     * Constructor cũ, nhận vào một chuỗi message.
     * Được giữ lại để tương thích ngược với các phần code cũ (như AuthService hiện tại).
     * Nó sẽ sử dụng một mã lỗi chung là "VALIDATION_ERROR".
     * @param message Thông báo lỗi chi tiết.
     */
    public AuthValidationException(String message) {
        super("VALIDATION_ERROR", message, 400);
    }
}
