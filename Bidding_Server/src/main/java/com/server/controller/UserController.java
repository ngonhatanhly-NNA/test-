package com.server.controller;

import com.google.gson.Gson;
import com.server.model.Role;
import com.server.service.UserService;
import com.shared.dto.*;
import com.shared.network.Response;

import java.util.Map;

public class UserController {
    private final UserService userService;
    private final Gson gson;

    /**
     * Constructor mặc định, tự khởi tạo service.
     * Được giữ lại để tương thích với code hiện tại.
     */
    public UserController() {
        this.userService = new UserService();
        this.gson = new Gson();
    }

    /**
     * Constructor dùng cho việc test.
     * Cho phép "tiêm" một phiên bản giả (mock) của UserService từ bên ngoài.
     * @param userService Service sẽ được sử dụng bởi controller.
     */
    public UserController(UserService userService) {
        this.userService = userService;
        this.gson = new Gson();
    }


    // 1. GET PROFILE (Trả về String JSON cho Router/Socket gửi về Client)
    public String handleGetUserProfile(String username) {
        // Lấy Object Response từ Service
        Response response = userService.getUserProfile(username);
        // Đóng gói thành JSON
        return gson.toJson(response);
    }

    // 2. UPDATE PROFILE
    public String handleUpdateProfile(String username, String jsonBody) {
        try {
            Role userRole = userService.getUserRole(username);
            if (userRole == null) {
                return gson.toJson(new Response("FAIL", "Người dùng không tồn tại", null));
            }

            // Ép kiểu JSON thành Object DTO tương ứng với Role
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

            // Giao Object DTO cho Service xử lý
            Response response = userService.updateProfile(username, updateDto);

            // Đóng gói kết quả thành JSON
            return gson.toJson(response);

        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new Response("ERROR", "Dữ liệu JSON không hợp lệ: " + e.getMessage(), null));
        }
    }

    // 3. ĐỔI MẬT KHẨU
    public String handleChangePassword(String username, String jsonBody) {
        try {
            // Giải nén JSON lấy mật khẩu cũ và mới
            Map<String, String> passwords = gson.fromJson(jsonBody, Map.class);
            String oldPass = passwords.get("oldPass");
            String newPass = passwords.get("newPass");

            if (oldPass == null || newPass == null) {
                return gson.toJson(new Response("FAIL", "Thiếu thông tin mật khẩu", null));
            }

            // Giao cho Service xử lý
            Response response = userService.changePassword(username, oldPass, newPass);
            return gson.toJson(response);

        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi định dạng JSON: " + e.getMessage(), null));
        }
    }
}
