package com.server.DAO;

import com.server.model.Item;
import com.server.model.Seller;

import java.util.List;
import java.util.Map;

/**
 * Interface định nghĩa các hành động truy xuất database cho Seller.
 */
public interface ISellerRepository {

    boolean promoteToSeller(long userId, String shopName, String bankAccountNumber);

    boolean updateShopDetails(long sellerId, String newShopName, String newBankAccount);

    boolean updateRating(long sellerId, double newRating, int newTotalReviews);

    Seller findSellerByUserId(long userId);

    /**
     * Lấy danh sách items thuộc về seller qua bảng auctions (seller_id).
     * Vì bảng items không có seller_id, ta lấy qua auctions.seller_id.
     */
    List<Item> getItemsBySellerId(long sellerId);

    /**
     * Lấy thống kê bán hàng của seller:
     * - Tổng số phiên đấu giá
     * - Tổng số phiên đã hoàn thành (CLOSED/COMPLETED)
     * - Tổng doanh thu (tổng highest bid đã thắng)
     * - Số item đang đấu giá (ACTIVE)
     */
    Map<String, Object> getSellerStatistics(long sellerId);
}