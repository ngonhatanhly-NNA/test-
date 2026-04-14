package com.server.exception;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String username) {
        super("USER_NOT_FOUND", "Không tìm thấy người dùng có tài khoản: " + username, 404);
    }
}