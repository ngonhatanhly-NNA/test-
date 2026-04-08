package com.server.service;

import com.google.gson.Gson;
import com.server.DAO.UserRepository;
import com.server.model.Bidder;
import com.server.model.User;
import com.shared.dto.UserProfileUpdateDTO;
import com.shared.network.Response;

public class UserService {
    private UserRepository userRepository;
    private final Gson gson;

    public UserService() {
        this.userRepository = new UserRepository();
        this.gson = new Gson();
    }

    public String getUserProfile(String username) {
        try {
            // Xuống DB tìm user theo username
            User user = userRepository.getUserByUsername(username);

            if (user != null) {
                // Đóng gói thành công
                return gson.toJson(new Response("SUCCESS", "Lấy profile thành công", user));
            }
            return gson.toJson(new Response("FAIL", "Không tìm thấy người dùng", null));
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi hệ thống: " + e.getMessage(), null));
        }
    }

    public String updateProfile(String jsonBody) {
        try {
            UserProfileUpdateDTO updatedInfo = gson.fromJson(jsonBody, UserProfileUpdateDTO.class);

            // Lấy user cũ từ DB lên để đối chiếu
            User existingUser = userRepository.getUserByUsername(updatedInfo.getUsername());

            if (existingUser != null) {
                // Set lại các trường được phép đổi
                existingUser.setEmail(updatedInfo.getEmail());
                existingUser.setPhoneNumber(updatedInfo.getPhoneNumber());
                existingUser.setAddress(updatedInfo.getAddress());

                // Sai DAO đem cất bản cập nhật này xuống Database
                boolean isSuccess = userRepository.updateUser(existingUser);

                if (isSuccess) {
                    return gson.toJson(new Response("SUCCESS", "Cập nhật thông tin thành công!", existingUser));
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