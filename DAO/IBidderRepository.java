package com.server.DAO;

import com.server.model.Bidder;
import java.math.BigDecimal;

public interface IBidderRepository {
    // Cập nhật số dư (Nạp/Trừ tiền)
    boolean updateBalance(long bidderId, BigDecimal newBalance);

    // Tìm Bidder theo Username (Dùng khi đăng nhập/xem profile)
    Bidder getBidderByUsername(String username);

    // Tìm Bidder theo ID
    Bidder getBidderById(long id);
}