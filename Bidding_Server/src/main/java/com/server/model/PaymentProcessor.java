package com.server.model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public interface PaymentProcessor {
    boolean processDeposit(long userId, double amount);
}

// VNPayAdapter triển khai interface xử lý thanh toán
class VNPayAdapter implements PaymentProcessor {
    @Override
    public boolean processDeposit(long userId, double amount) {
        Logger logger = LoggerFactory.getLogger(VNPayAdapter.class);
        logger.info("Đang mã hóa dữ liệu...");
        logger.info("Thực hiện nạp {} VNĐ.", amount);
        return true;
    }
}

// triener khai adpater de sau co nhieu phuong thuc thanh toan...
class MomoAdapter implements PaymentProcessor {
    @Override
    public boolean processDeposit(long userId, double amount) {
        Logger logger = LoggerFactory.getLogger(MomoAdapter.class);
        logger.info("Đang xử lý thanh toán qua Momo...");
        logger.info("Thực hiện nạp {} VNĐ.", amount);
        return true;
    }
}