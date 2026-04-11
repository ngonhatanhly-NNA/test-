package com.server.service;

import com.google.gson.Gson;
import com.server.DAO.BidderRepository;
import com.server.DAO.IBidderRepository;
import com.server.model.Bidder;
import com.shared.network.Response;
import java.math.BigDecimal;

public class BidderService {
    private final Gson gson = new Gson();
    private final IBidderRepository bidderRepo = new BidderRepository();

    /**
     * NẠP TIỀN VÀ LƯU DATABASE
     */
    public String depositMoney(Bidder bidder, double amount) {
        try {
            if (amount <= 0) {
                return gson.toJson(new Response("FAIL", "Số tiền nạp phải lớn hơn 0!", null));
            }

            BigDecimal depositAmount = BigDecimal.valueOf(amount);

            // 1. Cộng tiền vào Object (RAM)
            bidder.addFunds(depositAmount);

            // 2. Lưu vào Database (Liên kết thật)
            boolean isSaved = bidderRepo.updateBalance(bidder.getId(), bidder.getWalletBalance());

            if (isSaved) {
                return gson.toJson(new Response("SUCCESS", "Nạp tiền thành công! Số dư: " + bidder.getWalletBalance(), bidder));
            } else {
                // Rollback Object nếu DB lỗi
                bidder.setWalletBalance(bidder.getWalletBalance().subtract(depositAmount));
                return gson.toJson(new Response("ERROR", "Lỗi: Không thể cập nhật Database!", null));
            }
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi: " + e.getMessage(), null));
        }
    }

    /**
     * KIỂM TRA TÀI CHÍNH (Lấy số dư mới nhất từ DB)
     */
    public boolean canAffordBid(Bidder bidder, double requiredAmount) {
        Bidder latest = bidderRepo.getBidderById(bidder.getId());
        if (latest != null) {
            bidder.setWalletBalance(latest.getWalletBalance());
        }
        return bidder.getWalletBalance().compareTo(BigDecimal.valueOf(requiredAmount)) >= 0;
    }

    /**
     * TRỪ TIỀN KHI THẮNG ĐẤU GIÁ
     */
    public void settlePayment(Bidder winner, double finalAmount) {
        BigDecimal amountToDeduct = BigDecimal.valueOf(finalAmount);

        if (winner.deductFunds(amountToDeduct)) {
            boolean success = bidderRepo.updateBalance(winner.getId(), winner.getWalletBalance());
            if (success) {
                System.out.println("Đã thanh toán thành công cho: " + winner.getUsername());
            } else {
                System.out.println("Lỗi DB: Chưa trừ được tiền người thắng!");
            }
        }
    }
}