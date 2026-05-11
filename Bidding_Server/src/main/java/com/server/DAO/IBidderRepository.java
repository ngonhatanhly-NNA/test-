package com.server.DAO;

import com.server.model.Bidder;
import java.math.BigDecimal;

public interface IBidderRepository {
    // Cập nhật số dư (Nạp/Trừ tiền)
    boolean updateBalance(long bidderId, BigDecimal newBalance);

    // Thêm mới: Update cả ví chính và ví tạm thời cùng lúc (Dùng khi cần đồng bộ 2 ví)
    boolean updateWalletBalances(long bidderId, BigDecimal mainBalance, BigDecimal tempBalance);

    // Tìm Bidder theo Username (Dùng khi đăng nhập/xem profile)
    Bidder getBidderByUsername(String username);

    // Tìm Bidder theo ID
    Bidder getBidderById(long id);
}