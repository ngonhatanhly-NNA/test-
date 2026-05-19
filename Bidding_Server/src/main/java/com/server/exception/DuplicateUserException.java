package com.server.exception;

/**
 * Exception cho các lỗi liên quan đến việc tạo tài nguyên đã tồn tại (ví dụ: trùng username, email).
 * Kế thừa từ AppException để được xử lý nhất quán.
 */
public class DuplicateUserException extends AppException {

    /**
     * Enum để xác định cụ thể trường nào bị trùng lặp.
     */
    public enum ErrorCode {
        USERNAME_EXISTED("Tài khoản đã tồn tại"),
        EMAIL_EXISTED("Email đã được sử dụng");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Constructor để tạo exception với thông tin chi tiết.
     * @param code Mã lỗi cụ thể (ví dụ: USERNAME_EXISTED).
     * @param detail Giá trị bị trùng lặp (ví dụ: username "testuser").
     */
    public DuplicateUserException(ErrorCode code, String detail) {
        // Gọi constructor của lớp cha (AppException).
        // - code.name() sẽ là mã lỗi (ví dụ: "USERNAME_EXISTED").
        // - Message sẽ được ghép lại để có thông tin chi tiết (ví dụ: "Tài khoản đã tồn tại: 'testuser'").
        // - HttpStatus 409 (Conflict) là mã lỗi phù hợp cho trường hợp này.
        super(code.name(), code.getMessage() + ": '" + detail + "'", 409);
    }
}
