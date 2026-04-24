package com.server.service;

import com.server.DAO.ISellerRepository;
import com.server.model.Seller;
import com.server.model.Status;
import com.server.model.User;
import com.shared.network.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Lớp Service xử lý các nghiệp vụ liên quan đến Seller,
 * giao tiếp với tầng Database thông qua Repository.
 */
public class SellerService {

    private final ISellerRepository sellerRepository;
    private static final Logger logger = LoggerFactory.getLogger(SellerService.class);
    // Sử dụng Dependency Injection để truyền Repository vào
    public SellerService(ISellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    /**
     * Cập nhật thông tin cửa hàng và lưu vào DB.
     * @param sellerId ID của người bán.
     * @param newShopName Tên cửa hàng mới.
     * @param newBankAccount Tài khoản ngân hàng mới.
     * @return Response cho biết thành công hay thất bại.
     */
    public Response updateShopInfo(long sellerId, String newShopName, String newBankAccount) {
        if (newShopName == null || newShopName.trim().isEmpty()) {
            return new Response("FAIL", "Tên cửa hàng không được để trống!", null);
        }
        boolean success = sellerRepository.updateShopDetails(sellerId, newShopName.trim(), newBankAccount);
        if (success) {
            return new Response("SUCCESS", "Cập nhật thông tin cửa hàng thành công!", null);
        } else {
            return new Response("ERROR", "Lỗi khi cập nhật database.", null);
        }
    }

    /**
     * Thêm một đánh giá mới cho Seller và cập nhật vào DB.
     * @param sellerId ID của người bán được đánh giá.
     * @param score Điểm số của đánh giá (từ 1-5).
     * @return Response cho biết thành công hay thất bại.
     */
    public Response addReview(long sellerId, double score) {
        // 1. Lấy thông tin seller hiện tại từ DB để có rating và totalReviews cũ
        Seller seller = sellerRepository.findSellerByUserId(sellerId);
        if (seller == null) {
            return new Response("FAIL", "Không tìm thấy người bán!", null);
        }

        // 2. Dùng logic trong model để tính toán rating mới (thao tác trên RAM)
        seller.addReviewScore(score);

        // 3. Lưu rating và totalReviews mới vào DB
        boolean success = sellerRepository.updateRating(sellerId, seller.getRating(), seller.getTotalReviews());

        if (success) {
            return new Response("SUCCESS", "Cảm ơn bạn đã đánh giá!", null);
        } else {
            return new Response("ERROR", "Lỗi khi lưu đánh giá vào database.", null);
        }
    }

    /**
     * Lấy thông tin chi tiết của một Seller.
     * @param sellerId ID của người bán.
     * @return Response chứa thông tin Seller.
     */
    public Response getSellerDetails(long sellerId) {
        Seller seller = sellerRepository.findSellerByUserId(sellerId);
        if (seller != null) {
            // Dùng lớp tĩnh SellerInfo để đóng gói dữ liệu trả về
            SellerInfo info = new SellerInfo(seller);
            return new Response("SUCCESS", "Lấy thông tin người bán thành công.", info);
        } else {
            return new Response("FAIL", "Không tìm thấy người bán.", null);
        }
    }


    // --- CÁC LỚP VÀ HÀM TIỆN ÍCH TĨNH (GIỮ NGUYÊN) ---

    /**
     * Hàm tiện ích để kiểm tra một User có phải là Seller hay không.
     */
    public static boolean isSeller(User user) {
        return user instanceof Seller;
    }

    /**
     * Lớp Helper để đóng gói thông tin của Seller một cách an toàn,
     * chỉ hiển thị những gì cần thiết cho Client.
     */
    public static class SellerInfo {
        public final String shopName;
        public final String ownerName;
        public final String rating;
        public final Status status;
        public final boolean hasBankAccount;
        public final boolean isVerified;

        public SellerInfo(Seller seller) {
            this.shopName = seller.getShopName();
            this.ownerName = seller.getFullName();
            this.rating = String.format("%.1f Sao (%d đánh giá)", seller.getRating(), seller.getTotalReviews());
            this.status = seller.getStatus();
            this.hasBankAccount = seller.getBankAccountNumber() != null && !seller.getBankAccountNumber().isEmpty();
            this.isVerified = seller.isVerified();
        }

        @Override
        public String toString() {
            return String.format("Cửa hàng: %s | Chủ sở hữu: %s | Đánh giá: %s | Trạng thái: %s",
                    shopName, ownerName, rating, status);
        }
    }
}