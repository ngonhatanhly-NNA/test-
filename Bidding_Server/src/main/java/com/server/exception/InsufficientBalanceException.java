package com.server.exception;

public class InsufficientBalanceException extends AppException {
    public InsufficientBalanceException() {
        super("INSUFFICIENT_BALANCE", "Số dư trong ví không đủ để thực hiện giao dịch này!");
    }
}