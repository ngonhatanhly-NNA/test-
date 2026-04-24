package com.server.service;

import com.server.DAO.UserRepository;
import com.server.model.*;
import com.shared.dto.*;
import com.shared.network.Response;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {   
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // ==========================================
    // MAPPER, OCP
    // ==========================================
    private interface ProfileMapper {
        Map<String, Object> map(User user);
    }

    private static final Map<Class<? extends User>, ProfileMapper> mappers = new HashMap<>();

    static {
        mappers.put(Admin.class, user -> {
            Admin admin = (Admin) user;
            Map<String, Object> map = createBaseProfileMap(admin);
            map.put("roleLevel", admin.getRoleLevel());
            map.put("lastLoginIp", admin.getLastLoginIp());
            return map;
        });

        mappers.put(Bidder.class, user -> {
            Bidder bidder = (Bidder) user;
            Map<String, Object> map = createBaseProfileMap(bidder);
            map.put("walletBalance", bidder.getWalletBalance());
            map.put("creditCardInfo", bidder.getCreditCardInfo());
            return map;
        });

        mappers.put(Seller.class, user -> {
            Seller seller = (Seller) user;
            Map<String, Object> map = createBaseProfileMap(seller);
            map.put("walletBalance", seller.getWalletBalance());
            map.put("creditCardInfo", seller.getCreditCardInfo());
            map.put("shopName", seller.getShopName());
            map.put("bankAccountNumber", seller.getBankAccountNumber());
            map.put("rating", seller.getRating());
            map.put("totalReviews", seller.getTotalReviews());
            map.put("isVerified", seller.isVerified());
            return map;
        });
    }

    public static void registerMapper(Class<? extends User> clazz, ProfileMapper mapper) {
        mappers.put(clazz, mapper);
    }

    // Hàm tạo base map dùng chung
    private static Map<String, Object> createBaseProfileMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("phoneNumber", user.getPhoneNumber());
        map.put("address", user.getAddress());
        map.put("role", user.getRole().name());
        map.put("status", user.getStatus().name());
        return map;
    }

    // ==========================================
    // Logic HAnlde here
    // ==========================================
    public UserService() {
        this.userRepository = new UserRepository();
    }

    public Role getUserRole(String username) {
        User user = userRepository.getUserByUsername(username);
        return (user != null) ? user.getRole() : null;
    }

    public Response getUserProfile(String username) {
        try {
            User user = userRepository.getUserByUsername(username);
            if (user != null) {
                ProfileMapper mapper = mappers.get(user.getClass());

                if (mapper != null) {
                    return new Response("SUCCESS", "Lấy profile thành công", mapper.map(user));
                } else {
                    // Fallback: Nếu lỡ thiếu mapper, trả về thông tin cơ bản
                    return new Response("SUCCESS", "Lấy profile cơ bản", createBaseProfileMap(user));
                }
            }
            return new Response("FAIL", "Không tìm thấy người dùng", null);
        } catch (Exception e) {
            return new Response("ERROR", "Lỗi hệ thống: " + e.getMessage(), null);
        }
    }

    public Response updateProfile(String username, BaseProfileUpdateDTO updatedInfo) {
        try {
            User existingUser = userRepository.getUserByUsername(username);

            if (existingUser != null) {
                boolean isSuccess = userRepository.updateFullProfile(updatedInfo, existingUser.getRole(), existingUser.getId());
                if (isSuccess) {
                    return new Response("SUCCESS", "Cập nhật thông tin thành công!", null);
                } else {
                    return new Response("FAIL", "Lỗi khi lưu vào Database", null);
                }
            }
            return new Response("FAIL", "Người dùng không tồn tại", null);
        } catch (Exception e) {
            return new Response("ERROR", "Lỗi cập nhật: " + e.getMessage(), null);
        }
    }

    public Response changePassword(String username, String oldPass, String newPass) {
        try {
            User user = userRepository.getUserByUsername(username);

            if (user != null && isPasswordMatch(oldPass, user.getPasswordHash())) {
                String newPasswordHash = BCrypt.hashpw(newPass, BCrypt.gensalt());
                boolean isSuccess = userRepository.updatePassword(username, newPasswordHash);
                if (isSuccess) {
                    return new Response("SUCCESS", "Đổi mật khẩu thành công!", null);
                } else {
                    return new Response("FAIL", "Lỗi khi lưu mật khẩu mới", null);
                }
            }
            return new Response("FAIL", "Mật khẩu cũ không chính xác!", null);
        } catch (Exception e) {
            return new Response("ERROR", "Lỗi: " + e.getMessage(), null);
        }
    }

    private boolean isPasswordMatch(String rawPassword, String storedHash) {
        if (storedHash == null || storedHash.trim().isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            // Legacy fallback: stored password may be plaintext
            return storedHash.equals(rawPassword);
        }
    }

    /**
     * Xử lý yêu cầu nâng cấp một BIDDER thành SELLER.
     * @param username Tên đăng nhập của người dùng gửi yêu cầu.
     * @return Một đối tượng Response cho biết kết quả.
     */
    public Response requestSellerUpgrade(String username) {
        try {
            // 1. Tìm người dùng trong CSDL
            User user = userRepository.getUserByUsername(username);
            if (user == null) {
                return new Response("FAIL", "Người dùng không tồn tại.", null);
            }

            // 2. Kiểm tra xem có đúng là BIDDER không
            if (user.getRole() != Role.BIDDER) {
                return new Response("FAIL", "Chỉ có Người mua (Bidder) mới có thể yêu cầu trở thành Người bán.", null);
            }

            // 3. Thực hiện nâng cấp vai trò
            boolean success = userRepository.updateUserRole(username, Role.SELLER);

            if (success) {
                // Lấy lại thông tin profile mới đã được cập nhật để gửi về client
                Response profileResponse = getUserProfile(username);
                logger.info("Nâng cấp vai trò thành công cho user '{}'", username);
                return new Response("SUCCESS", "Yêu cầu của bạn đã được chấp thuận. Bạn đã trở thành Người bán!", profileResponse.getData());
            } else {
                logger.warn("Nâng cấp vai trò thất bại cho user '{}' tại tầng repository", username);
                return new Response("ERROR", "Không thể cập nhật vai trò. Vui lòng thử lại.", null);
            }

        } catch (Exception e) {
            logger.error("Lỗi hệ thống khi nâng cấp vai trò cho user '{}': {}", username, e.getMessage(), e);
            return new Response("ERROR", "Đã xảy ra lỗi hệ thống: " + e.getMessage(), null);
        }
    }
}
