package com.server.service; // Đặt đúng package service

import com.google.gson.Gson;
import com.server.model.Auction;
import com.server.model.Bidder;
import com.shared.network.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BidderService {
    private final Gson gson = new Gson();

    /**
     * 1. THUẬT TOÁN NẠP TIỀN
     * Đã sửa: Truyền double vào hàm addFunds đã tối ưu ở class Bidder
     */
    public String depositMoney(Bidder bidder, double amount) {
        try {
            if (amount <= 0) {
                return gson.toJson(new Response("FAIL", "Số tiền nạp phải lớn hơn 0!", null));
            }

            // Gọi hàm addFunds (nạp double, bên trong tự chuyển sang BigDecimal)
            bidder.addFunds(BigDecimal.valueOf(amount));

            return gson.toJson(new Response("SUCCESS", "Nạp tiền thành công! Số dư: " + bidder.getWalletBalance(), bidder));
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi hệ thống: " + e.getMessage(), null));
        }
    }

    /**
     * 2. KIỂM TRA NĂNG LỰC TÀI CHÍNH
     * Đã sửa: Dùng compareTo để so sánh BigDecimal với double
     */
    public boolean canAffordBid(Bidder bidder, double requiredAmount) {
        // BigDecimal.valueOf(requiredAmount) giúp so sánh chính xác
        return bidder.getWalletBalance().compareTo(BigDecimal.valueOf(requiredAmount)) >= 0;
    }

    /**
     * 3. TRUY XUẤT LỊCH SỬ CÁ NHÂN
     * Đã sửa: So sánh theo Username (vì AuctionItem.BidHistory lưu tên người dùng)
     */
    public String getMyBidHistory(Bidder bidder, List<AuctionItem> globalAuctions) {
        try {
            List<AuctionItem.BidHistory> myHistory = new ArrayList<>();
            for (AuctionItem auction : globalAuctions) {
                if (auction.getHistory() != null) {
                    // Lọc lịch sử dựa trên username của bidder
                    List<AuctionItem.BidHistory> personalBids = auction.getHistory().stream()
                            .filter(t -> t.bidder != null && t.bidder.equals(bidder.getUsername()))
                            .collect(Collectors.toList());
                    myHistory.addAll(personalBids);
                }
            }
            return gson.toJson(new Response("SUCCESS", "Lấy lịch sử thành công!", myHistory));
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi: " + e.getMessage(), null));
        }
    }

    /**
     * 4. THUẬT TOÁN TRỪ TIỀN KHI THẮNG
     * Đã sửa: Gọi deductFunds với kiểu double
     */
    public void settlePayment(Bidder winner, double finalAmount) {
        boolean success = winner.deductFunds(BigDecimal.valueOf(finalAmount));

        if (success) {
            System.out.println("Thanh toán thành công cho người thắng: " + winner.getUsername());
        } else {
            System.out.println("Thanh toán thất bại: Số dư không đủ hoặc số tiền lỗi!");
        }
    }
}
