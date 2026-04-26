package com.server.service;

import com.server.DAO.ISellerRepository;
import com.server.model.Item;
import com.server.model.Seller;
import com.server.model.Status;
import com.server.model.User;
import com.shared.dto.ItemResponseDTO;
import com.shared.network.Response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp Service xử lý các nghiệp vụ liên quan đến Seller,
 * giao tiếp với tầng Database thông qua Repository.
 */
public class SellerService {

    private final ISellerRepository sellerRepository;
    private static final Logger logger = LoggerFactory.getLogger(SellerService.class);

    public SellerService(ISellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    /**
     * Lấy thông tin chi tiết của một Seller.
     */
    public Response getSellerDetails(long sellerId) {
        Seller seller = sellerRepository.findSellerByUserId(sellerId);
        if (seller != null) {
            SellerInfo info = new SellerInfo(seller);
            return new Response("SUCCESS", "Lấy thông tin người bán thành công.", info);
        } else {
            return new Response("FAIL", "Không tìm thấy người bán.", null);
        }
    }

    /**
     * Lấy danh sách items của seller (qua bảng auctions).
     */
    public Response getSellerItems(long sellerId) {
        List<Item> items = sellerRepository.getItemsBySellerId(sellerId);
        List<ItemResponseDTO> dtoList = items.stream().map(item -> {
            String type = item.getClass().getSimpleName().toUpperCase();
            return new ItemResponseDTO(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getStartingPrice(),
                    type,
                    item.getImageUrls()
            );
        }).collect(Collectors.toList());
        return new Response("SUCCESS", "Lấy danh sách sản phẩm thành công.", dtoList);
    }

    /**
     * Lấy thống kê bán hàng của seller.
     */
    public Response getSellerStatistics(long sellerId) {
        Map<String, Object> stats = sellerRepository.getSellerStatistics(sellerId);
        if (stats != null) {
            return new Response("SUCCESS", "Lấy thống kê thành công.", stats);
        }
        return new Response("FAIL", "Không thể lấy thống kê.", null);
    }

    /**
     * Cập nhật thông tin cửa hàng và lưu vào DB.
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
     * Thêm một đánh giá mới cho Seller.
     */
    public Response addReview(long sellerId, double score) {
        Seller seller = sellerRepository.findSellerByUserId(sellerId);
        if (seller == null) {
            return new Response("FAIL", "Không tìm thấy người bán!", null);
        }
        seller.addReviewScore(score);
        boolean success = sellerRepository.updateRating(sellerId, seller.getRating(), seller.getTotalReviews());
        if (success) {
            return new Response("SUCCESS", "Cảm ơn bạn đã đánh giá!", null);
        } else {
            return new Response("ERROR", "Lỗi khi lưu đánh giá vào database.", null);
        }
    }

    public static boolean isSeller(User user) {
        return user instanceof Seller;
    }

    /**
     * Lớp Helper để đóng gói thông tin Seller trả về cho Client.
     */
    public static class SellerInfo {
        public final String shopName;
        public final String ownerName;
        public final double rating;
        public final int totalReviews;
        public final String ratingDisplay;
        public final Status status;
        public final boolean hasBankAccount;
        public final boolean isVerified;
        public final String bankAccountNumber;

        public SellerInfo(Seller seller) {
            this.shopName = seller.getShopName();
            this.ownerName = seller.getFullName();
            this.rating = seller.getRating();
            this.totalReviews = seller.getTotalReviews();
            this.ratingDisplay = String.format("%.1f ★ (%d đánh giá)", seller.getRating(), seller.getTotalReviews());
            this.status = seller.getStatus();
            this.hasBankAccount = seller.getBankAccountNumber() != null && !seller.getBankAccountNumber().isEmpty();
            this.isVerified = seller.isVerified();
            this.bankAccountNumber = seller.getBankAccountNumber();
        }
    }
}