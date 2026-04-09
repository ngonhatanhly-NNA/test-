package com.server.exception;


public class DuplicateUserException extends AppException {
    public DuplicateUserException(String username) {
        super("USER_EXISTED", "Tài khoản '" + username + "' đã tồn tại trên hệ thống!");
    }
}