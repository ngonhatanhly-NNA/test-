package com.server.model;

public interface PaymentProcessor {
    boolean processDeposit(long userId, double amount);
}

// VNPayAdapter triển khai interface xử lý thanh toán
class VNPayAdapter implements PaymentProcessor {
    @Override
    public boolean processDeposit(long userId, double amount) {
        System.out.println("Đang mã hóa dữ liệu gửi sang VNPay...");
        System.out.println("Thực hiện nạp " + amount + " VNĐ qua VNPay.");
        return true;
    }
}

// triener khai adpater de sau co nhieu phuong thuc thanh toan...
class MomoAdapter implements PaymentProcessor {
    @Override
    public boolean processDeposit(long userId, double amount) {
        System.out.println("Gọi API Momo...");
        return true;
    }
}