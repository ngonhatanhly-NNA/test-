package com.server.service;

import com.google.gson.Gson;
import com.server.DAO.BidderRepository;
import com.server.DAO.IBidderRepository;
import com.server.model.Bidder;
import com.shared.dto.WalletTransactionDTO;
import com.shared.network.Response;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BidderService {
    private final Gson gson = new Gson();
    private final IBidderRepository bidderRepo;
    private static final Logger logger = LoggerFactory.getLogger(BidderService.class);

    // Constructor mặc định (dùng trong production)
    public BidderService() {
        this.bidderRepo = new BidderRepository();
    }

    // Constructor để Dependency Injection (dùng trong testing với Mock)
    public BidderService(IBidderRepository bidderRepo) {
        this.bidderRepo = bidderRepo;
    }

    /**
     * NẠP TIỀN VÀ LƯU DATABASE (Overload 1: với Bidder object)
     * Sau khi nạp thành công, ghi log giao dịch vào wallet_transactions.
     */
    public String depositMoney(Bidder bidder, double amount) {
        try {
            if (amount <= 0) {
                return gson.toJson(new Response("FAIL", "Số tiền nạp phải lớn hơn 0!", null));
            }

            BigDecimal depositAmount = BigDecimal.valueOf(amount);

            // 1. Cộng tiền vào Object (RAM)
            bidder.addFunds(depositAmount);

            // 2. Lưu vào Database
            boolean isSaved = bidderRepo.updateBalance(bidder.getId(), bidder.getWalletBalance());

            if (isSaved) {
                // 3. Ghi log giao dịch nạp tiền
                String description = String.format("Nạp tiền vào ví: +%,.0f VNĐ", amount);
                bidderRepo.logTransaction(bidder.getId(), depositAmount, "DEPOSIT", description);

                return gson.toJson(new Response("SUCCESS", "Nạp tiền thành công! Số dư: " + bidder.getWalletBalance(), bidder));
            } else {
                // Rollback Object nếu DB lỗi
                bidder.setWalletBalance(bidder.getWalletBalance().subtract(depositAmount));
                logger.error("Lỗi: Không thể cập nhật Database!");
                return gson.toJson(new Response("ERROR", "Lỗi: Không thể cập nhật Database!", null));
            }
        } catch (Exception e) {
            logger.error("Lỗi: " + e.getMessage());
            return gson.toJson(new Response("ERROR", "Lỗi: " + e.getMessage(), null));
        }
    }

    /**
     * NẠP TIỀN VÀ LƯU DATABASE (Overload 2: với bidderId)
     * Dùng từ Controller khi chỉ có ID.
     */
    public String depositMoney(long bidderId, double amount) {
        try {
            if (amount <= 0) {
                return gson.toJson(new Response("FAIL", "Số tiền nạp phải lớn hơn 0!", null));
            }

            // Lấy Bidder từ DB
            Bidder bidder = bidderRepo.getBidderById(bidderId);
            if (bidder == null) {
                return gson.toJson(new Response("FAIL", "Không tìm thấy Bidder với ID: " + bidderId, null));
            }

            // Gọi overload 1
            return depositMoney(bidder, amount);
        } catch (Exception e) {
            logger.error("Lỗi nạp tiền cho bidder ID {}: {}", bidderId, e.getMessage());
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
     * TRỪ TIỀN KHI THẮNG ĐẤU GIÁ.
     * Sau khi trừ thành công, ghi log giao dịch vào wallet_transactions.
     *
     * @param winner      Bidder thắng đấu giá
     * @param finalAmount Số tiền cần thanh toán
     * @param itemName    Tên sản phẩm đấu giá (để ghi vào mô tả)
     */
    public void settlePayment(Bidder winner, double finalAmount, String itemName) {
        BigDecimal amountToDeduct = BigDecimal.valueOf(finalAmount);

        if (winner.deductFunds(amountToDeduct)) {
            boolean success = bidderRepo.updateBalance(winner.getId(), winner.getWalletBalance());
            if (success) {
                // Ghi log giao dịch trừ tiền
                String description = String.format("Thanh toán đấu giá thành công: %s (-%,.0f VNĐ)",
                        itemName != null ? itemName : "Không rõ", finalAmount);
                bidderRepo.logTransaction(winner.getId(), amountToDeduct, "DEDUCT", description);
                logger.info("Đã thanh toán thành công cho: " + winner.getUsername());
            } else {
                logger.error("Lỗi DB: Chưa trừ được tiền người thắng!");
            }
        }
    }

    /**
     * TRỪ TIỀN KHI THẮNG ĐẤU GIÁ (phiên bản tương thích ngược, không có itemName).
     * Được giữ lại để không phá vỡ code cũ đang gọi phương thức này.
     */
    public void settlePayment(Bidder winner, double finalAmount) {
        settlePayment(winner, finalAmount, null);
    }

    /**
     * LẤY LỊCH SỬ GIAO DỊCH VÍ của người dùng.
     * Bao gồm cả nạp tiền và trừ tiền khi thắng đấu giá.
     *
     * @param userId ID người dùng
     * @return Response chứa danh sách WalletTransactionDTO
     */
    public Response getWalletTransactions(long userId) {
        List<WalletTransactionDTO> transactions = bidderRepo.getWalletTransactions(userId);
        return new Response("SUCCESS", "Lấy lịch sử giao dịch ví thành công.", transactions);
    }

    /**
     * LẤY THÔNG TIN CHI TIẾT BIDDER (dùng cho profile hoặc wallet display)
     */
    public Bidder getBidderDetails(long bidderId) {
        return bidderRepo.getBidderById(bidderId);
    }
}