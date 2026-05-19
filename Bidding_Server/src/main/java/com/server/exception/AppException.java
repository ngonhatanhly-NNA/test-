package com.server.exception;

// Class cha quản lý mọi lỗi trong hệ thống
public class AppException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 400;
    }

    public AppException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}