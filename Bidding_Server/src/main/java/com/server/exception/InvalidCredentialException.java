package com.server.exception;
// sai pas
public class InvalidCredentialException extends AppException {
    public InvalidCredentialException() {
        super("INVALID_CREDENTIAL", "Tài khoản hoặc mật khẩu không chính xác!");
    }
}