package com.server.model;

public interface PaymentFactory {
    public boolean processDeposit(long userId, double amount);
}

// Giả ập các phương thức nạp tiền :))) Apdapter, Factory
class VNPayAdapter implements PaymentFactory {
        @Override
        public boolean processDeposit(long userId, double amount) {
            // Giả sử VNPay yêu cầu định dạng tiền tệ khác, hoặc phải mã hóa HMAC SHA512
            System.out.println("Đang mã hóa dữ liệu gửi sang VNPay...");
            System.out.println("Thực hiện nạp " + amount + " VNĐ qua VNPay.");
            return true; // Giả lập thành công
        }
    }
