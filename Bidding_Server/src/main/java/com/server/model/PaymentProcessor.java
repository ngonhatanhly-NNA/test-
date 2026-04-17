package com.server.model;

public interface PaymentProcessor {
    boolean processDeposit(long userId, double amount);
}

// VNPayAdapter triển khai interface xử lý thanh toán
class VNPayAdapter implements PaymentProcessor {
    @Override
    public boolean processDeposit(long userId, double amount) {
        System.out.println("Đang mã hóa dữ liệu...");
        System.out.println("Thực hiện nạp " + amount + " VNĐ.");
        return true;
    }
}

// triener khai adpater de sau co nhieu phuong thuc thanh toan...
class MomoAdapter implements PaymentProcessor {
    @Override
    public boolean processDeposit(long userId, double amount) {
        System.out.println("...");
        return true;
    }
}