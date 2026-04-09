package com.server.exception;

// Class cha quản lý mọi lỗi trong hệ thống
public class AppException extends RuntimeException {
    private final String errorCode;

    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}