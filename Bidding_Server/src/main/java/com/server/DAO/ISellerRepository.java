package com.server.DAO;

import com.server.model.Seller;
import java.math.BigDecimal;

/**
 * Interface định nghĩa các hành động truy xuất database cho Seller.
 */
public interface ISellerRepository {

    /**
     * Nâng cấp một Bidder thành Seller trong database.
     * Thao tác này thường bao gồm:
     * 1. Cập nhật role trong bảng 'users'.
     * 2. Thêm một bản ghi mới vào bảng 'sellers'.
     * @return true nếu thành công, false nếu thất bại.
     */
    boolean promoteToSeller(long userId, String shopName, String bankAccountNumber);

    /**
     * Cập nhật thông tin cửa hàng (tên, tài khoản ngân hàng) cho một Seller.
     * @return true nếu thành công, false nếu thất bại.
     */
    boolean updateShopDetails(long sellerId, String newShopName, String newBankAccount);

    /**
     * Cập nhật lại điểm đánh giá và tổng số lượt đánh giá cho Seller.
     * @return true nếu thành công, false nếu thất bại.
     */
    boolean updateRating(long sellerId, double newRating, int newTotalReviews);

    /**
     * Lấy thông tin của một Seller từ database dựa trên user_id.
     * @return một object Seller đầy đủ thông tin, hoặc null nếu không tìm thấy.
     */
    Seller findSellerByUserId(long userId);
}