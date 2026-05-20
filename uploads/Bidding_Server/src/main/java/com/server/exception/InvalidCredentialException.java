package com.server.exception;

/**
 * Exception được ném ra khi người dùng cung cấp thông tin đăng nhập (tài khoản, mật khẩu) không chính xác.
 * Thường dẫn đến mã lỗi HTTP 401 Unauthorized.
 */
public class InvalidCredentialException extends AppException {

    /**
     * Constructor mặc định với thông báo chung.
     */
    public InvalidCredentialException() {
        super("INVALID_CREDENTIALS", "Tài khoản hoặc mật khẩu không chính xác.", 401);
    }

    /**
     * Constructor với thông báo chi tiết hơn, ghi lại tên người dùng đã cố gắng đăng nhập.
     * Điều này rất hữu ích cho việc ghi log (ghi lại nhật ký) để theo dõi các lần đăng nhập thất bại.
     * @param username Tên tài khoản đã được sử dụng để đăng nhập.
     */
    public InvalidCredentialException(String username) {
        super("INVALID_CREDENTIALS", "Thông tin đăng nhập không hợp lệ cho người dùng: '" + username + "'", 401);
    }
}
