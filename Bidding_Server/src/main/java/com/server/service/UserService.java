package com.server.service;

import com.google.gson.Gson;
import com.server.DAO.UserRepository;
import com.server.model.Admin;
import com.server.model.Bidder;
import com.server.model.Seller;
import com.server.model.User;
import com.shared.dto.UserProfileUpdateDTO;
import com.shared.dto.UserProfileUpdateDTO;
import com.shared.network.Response;

public class UserService {
    private UserRepository userRepository;

    // DESIGN PATTERN: SINGLETON (Dùng 1 object duy nhất cho toàn server, tiết kiệm bộ nhớ)
    private static final Gson gson = new Gson();

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public String getUserProfile(String username) {
        try {
            // Xuống DB tìm user theo username
            User user = userRepository.getUserByUsername(username);

            if (user != null) {
                // Tuyệt đối không trả thẳng object User về vì sẽ lộ PasswordHash cho Hacker
                UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
                dto.setId((int) user.getId());
                dto.setUsername(user.getUsername());
                dto.setEmail(user.getEmail());
                dto.setFullName(user.getFullName());
                dto.setPhoneNumber(user.getPhoneNumber());
                dto.setAddress(user.getAddress());
                dto.setRole(user.getRole().name());
                dto.setStatus(user.getStatus().name());

                // Trích xuất thêm thông tin dựa theo tính đa hình (OOP)
                if (user instanceof Admin) {
                    dto.setRoleLevel(((Admin) user).getRoleLevel());
                    dto.setLastLoginIp(((Admin) user).getLastLoginIp());
                } else if (user instanceof Seller) {
                    Seller seller = (Seller) user;
                    dto.setWalletBalance(seller.getWalletBalance());
                    dto.setCreditCardInfo(seller.getCreditCardInfo());
                    dto.setShopName(seller.getShopName());
                    dto.setBankAccountNumber(seller.getBankAccountNumber());
                    dto.setRating(seller.getRating());
                    dto.setTotalReviews(seller.getTotalReviews());
                    dto.setIsVerified(seller.isVerified());
                } else if (user instanceof Bidder) {
                    Bidder bidder = (Bidder) user;
                    dto.setWalletBalance(bidder.getWalletBalance());
                    dto.setCreditCardInfo(bidder.getCreditCardInfo());
                }

                // Đóng gói DTO thành công
                return gson.toJson(new Response("SUCCESS", "Lấy profile thành công", dto));
            }
            return gson.toJson(new Response("FAIL", "Không tìm thấy người dùng", null));
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi hệ thống: " + e.getMessage(), null));
        }
    }

    // Yêu cầu Controller truyền thêm username vào (vì DTO Update không có trường username)
    public String updateProfile(String username, String jsonBody) {
        try {
            UserProfileUpdateDTO updatedInfo = gson.fromJson(jsonBody, UserProfileUpdateDTO.class);

            // Lấy user cũ từ DB lên để xác định Role và ID
            User existingUser = userRepository.getUserByUsername(username);

            if (existingUser != null) {
                // ĐỒNG BỘ: Gọi đúng hàm updateFullProfile của DAO (truyền DTO, Role và ID)
                boolean isSuccess = userRepository.updateFullProfile(updatedInfo, existingUser.getRole(), existingUser.getId());

                if (isSuccess) {
                    // Không trả lại object Entity, chỉ báo Success là đủ
                    return gson.toJson(new Response("SUCCESS", "Cập nhật thông tin thành công!", null));
                } else {
                    return gson.toJson(new Response("FAIL", "Lỗi khi lưu vào Database", null));
                }
            }
            return gson.toJson(new Response("FAIL", "Người dùng không tồn tại", null));
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi cập nhật: " + e.getMessage(), null));
        }
    }

    public String changePassword(String username, String oldPass, String newPass) {
        try {
            // Lấy user từ DB lên
            User user = userRepository.getUserByUsername(username);

            // Kiểm tra user có tồn tại và mật khẩu cũ có khớp không
            // Thực tế sau này chỗ này phải dùng hàm check Hash (vd: BCrypt.checkpw)
            if (user != null && user.getPasswordHash().equals(oldPass)) {

                // Gọi DAO cập nhật thẳng mật khẩu mới xuống DB
                boolean isSuccess = userRepository.updatePassword(username, newPass);

                if (isSuccess) {
                    return gson.toJson(new Response("SUCCESS", "Đổi mật khẩu thành công!", null));
                } else {
                    return gson.toJson(new Response("FAIL", "Lỗi khi lưu mật khẩu mới vào Database", null));
                }
            }
            return gson.toJson(new Response("FAIL", "Mật khẩu cũ không chính xác!", null));
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi: " + e.getMessage(), null));
        }
    }
}