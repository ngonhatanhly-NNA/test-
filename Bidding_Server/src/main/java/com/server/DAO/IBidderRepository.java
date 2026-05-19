package com.server.DAO;

import com.server.model.Bidder;
import com.shared.dto.WalletTransactionDTO;

import java.math.BigDecimal;
import java.util.List;

public interface IBidderRepository {
    // Cập nhật số dư (Nạp/Trừ tiền)
    boolean updateBalance(long bidderId, BigDecimal newBalance);

    // Thêm mới: Update cả ví chính và ví tạm thời cùng lúc (Dùng khi cần đồng bộ 2 ví)
    boolean updateWalletBalances(long bidderId, BigDecimal mainBalance, BigDecimal reservedAmount);

    // Tìm Bidder theo Username (Dùng khi đăng nhập/xem profile)
    Bidder getBidderByUsername(String username);

    // Tìm Bidder theo ID
    Bidder getBidderById(long id);

    /**
     * Ghi một giao dịch ví vào bảng wallet_transactions.
     * @param userId      ID người dùng
     * @param amount      Số tiền giao dịch (luôn dương)
     * @param type        "DEPOSIT" hoặc "DEDUCT"
     * @param description Mô tả giao dịch
     * @return true nếu ghi thành công
     */
    boolean logTransaction(long userId, BigDecimal amount, String type, String description);

    /**
     * Lấy lịch sử giao dịch ví của người dùng, sắp xếp mới nhất trước.
     * @param userId ID người dùng
     * @return danh sách các giao dịch
     */
    List<WalletTransactionDTO> getWalletTransactions(long userId);
}