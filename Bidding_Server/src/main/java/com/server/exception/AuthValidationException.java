package com.server.exception;

/** Dữ liệu đăng ký / đăng nhập không hợp lệ (thiếu trường, rỗng, v.v.) */
public class AuthValidationException extends AppException {
    public AuthValidationException(String message) {
        super("VALIDATION", message, 400);
    }
}
