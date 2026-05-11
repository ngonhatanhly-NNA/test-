package com.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.server.model.Role;
import com.server.service.UserService;
import com.shared.dto.*;
import com.shared.network.Response;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final Gson gson;

    public UserController() {
        this.userService = new UserService();
        this.gson = new Gson();
    }

    public UserController(UserService userService) {
        this.userService = userService;
        this.gson = new Gson();
    }

    public String handleGetUserProfile(String username) {
        Response response = userService.getUserProfile(username);
        return gson.toJson(response);
    }

    public String handleUpdateProfile(String username, String jsonBody) {
        try {
            Role userRole = userService.getUserRole(username);
            if (userRole == null) {
                return gson.toJson(new Response("FAIL", "Ngườii dùng không tồn tại", null));
            }

            BaseProfileUpdateDTO updateDto;
            switch (userRole) {
                case SELLER:
                    updateDto = gson.fromJson(jsonBody, SellerProfileUpdateDTO.class);
                    break;
                case ADMIN:
                    updateDto = gson.fromJson(jsonBody, AdminProfileUpdateDTO.class);
                    break;
                case BIDDER:
                default:
                    updateDto = gson.fromJson(jsonBody, BidderProfileUpdateDTO.class);
                    break;
            }

            Response response = userService.updateProfile(username, updateDto);
            return gson.toJson(response);

        } catch (Exception e) {
            logger.error("Lỗi cập nhật profile cho '{}': {}", username, e.getMessage(), e);
            return gson.toJson(new Response("ERROR", "Dữ liệu JSON không hợp lệ: " + e.getMessage(), null));
        }
    }

    public String handleChangePassword(String username, String jsonBody) {
        try {
            Map<String, String> passwords = gson.fromJson(jsonBody, Map.class);
            String oldPass = passwords.get("oldPass");
            String newPass = passwords.get("newPass");

            if (oldPass == null || newPass == null) {
                return gson.toJson(new Response("FAIL", "Thiếu thông tin mật khẩu", null));
            }

            Response response = userService.changePassword(username, oldPass, newPass);
            logger.info("Đổi mật khẩu thành công cho user '{}'", username);
            return gson.toJson(response);

        } catch (Exception e) {
            logger.error("Lỗi đổi mật khẩu cho '{}': {}", username, e.getMessage(), e);
            return gson.toJson(new Response("ERROR", "Lỗi định dạng JSON: " + e.getMessage(), null));
        }
    }

    /**
     * Xử lý yêu cầu nâng cấp thành Seller.
     * API Endpoint (ví dụ): POST /api/users/upgrade-to-seller
     * @param username Tên người dùng (có thể lấy từ token xác thực).
     * @return Chuỗi JSON chứa kết quả.
     */
    public String handleUpgradeToSeller(String username) {
        logger.info("Nhận được yêu cầu nâng cấp vai trò cho user '{}'", username);
        Response response = userService.requestSellerUpgrade(username);
        return gson.toJson(response);
    }

    /**
     * Xử lý yêu cầu cập nhật ảnh đại diện (Avatar)
     */
    public String handleUpdateAvatar(String jsonBody) {
        try {
            JsonObject data = gson.fromJson(jsonBody, JsonObject.class);
            String username = data.get("username").getAsString();
            String base64Image = data.get("base64Image").getAsString();

            // Tạo thư mục lưu trữ nếu chưa có
            String uploadDir = "uploads/avatars/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); 
            }

            // Giải mã chuỗi Base64 thành mảng byte
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            
            // 3. Lưu file vào ổ cứng
            String fileName = username + "_avatar.png";
            String filePath = uploadDir + fileName;
            Files.write(Paths.get(filePath), imageBytes);

            // 4. Gọi Service để cập nhật Database
            String dbPath = "/" + filePath; // Đường dẫn tương đối (Ví dụ: /uploads/avatars/admin_avatar.png)
            Response response = userService.updateAvatarUrl(username, dbPath);
            
            // Nếu thành công, nhét thêm cái dbPath vào data trả về để Client biết đường dẫn mới
            if ("SUCCESS".equals(response.getStatus())) {
                JsonObject resData = new JsonObject();
                resData.addProperty("avatarUrl", dbPath);
                response.setData(resData);
            }

            return gson.toJson(response);

        } catch (Exception e) {
            logger.error("Lỗi cập nhật Avatar: {}", e.getMessage(), e);
            return gson.toJson(new Response("ERROR", "Lỗi xử lý file trên Server: " + e.getMessage(), null));
        }
    }
}