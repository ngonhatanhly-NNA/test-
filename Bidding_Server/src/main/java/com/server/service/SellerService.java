
package com.server.service;
import com.server.model.*;

public class SellerService {
    public Seller upgradeBidderToSeller(Bidder bidder, String shopName, String bankAccount) {
        // Kiểm tra điều kiện trước khi nâng cấp
        if (!isValidForUpgrade(bidder)) {
            throw new IllegalArgumentException("Bidder không đủ điều kiện nâng cấp!");
        }
        Seller seller = new Seller(bidder, shopName, bankAccount);
        seller.setRole(Role.SELLER);
        return seller;
    }
    // Kiểm tra bidder có đủ điều kiện nâng cấp không
    private boolean isValidForUpgrade(Bidder bidder) {
        return "ACTIVE".equals(bidder.getStatus())
                && bidder.getEmail() != null
                && !bidder.getEmail().trim().isEmpty();
    }
    // 2. QUẢN LÝ THÔNG TIN CỬA HÀNG
    public void updateShop(Seller seller, String shopName, String bankAccount) {
        if (shopName != null && !shopName.trim().isEmpty()) {
            seller.setShopName(shopName.trim());
        }
        if (bankAccount != null && !bankAccount.trim().isEmpty()) {
            seller.setBankAccountNumber(bankAccount.trim());
        }
    }
    // 3. HỆ THỐNG ĐÁNH GIÁ
    // Model đã validate 1-5 sao sẵn
    public void addReview(Seller seller, double score) {
        seller.addReviewScore(score);
    }
    // 4. XÁC THỰC SELLER (Chỉ Admin)
    public boolean verifySeller(Admin admin, Seller seller) {
        if (!isAuthorizedAdmin(admin)) return false;

        // TODO: Thêm setVerified() vào model Seller
        seller.setRole(Role.SELLER);
        return true;
    }
    // Kiểm tra quyền Admin
    private boolean isAuthorizedAdmin(Admin admin) {
        return admin != null &&
                ("SUPER_ADMIN".equals(admin.getRoleLevel()) ||
                        "ADMIN".equals(admin.getRoleLevel()));
    }

    // 5. KIỂM TRA TRẠNG THÁI
    public boolean isActiveSeller(Seller seller) {
        return seller != null &&
                "ACTIVE".equals(seller.getStatus()) &&
                "SELLER".equals(seller.getRole()) &&
                seller.getShopName() != null;
    }
    // 6. THÔNG TIN & BÁO CÁO
    public SellerInfo getSellerInfo(Seller seller) {
        return new SellerInfo(seller);
    }


    // 7. TIỆN ÍCH TĨNH( không cần tạo đối tượng vẫn có thể gọi được)
    public static boolean isSeller(User user) {
        return user instanceof Seller seller && "SELLER".equals(seller.getRole());
    }
    // Lớp helper chứa thông tin seller
    public static class SellerInfo {
        public final String shopName;
        public final String ownerName;
        public final String rating;
        public final Status status;
        public final boolean hasBankAccount;

        public SellerInfo(Seller seller) {
            this.shopName = seller.getShopName();
            this.ownerName = seller.getFullName();
            this.rating = String.format("%.1f Sao (%d)", seller.getRating(), seller.getTotalReviews());
            this.status = seller.getStatus();
            this.hasBankAccount = seller.getBankAccountNumber() != null;
        }
        @Override
        public String toString() {
            return String.format("%s | %s | %s | %s",
                    shopName, ownerName, rating, status);
        }
    }
}

