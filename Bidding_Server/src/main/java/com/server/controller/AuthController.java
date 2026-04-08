package com.server.controller;

import com.server.service.AuthService;
import com.google.gson.Gson;
import com.shared.dto.*;
import com.shared.network.Response;

// Controller, nơi giao việc cho service, nối DB và (tùy hugnws) quản lí đăng nhập
public class AuthController{

    private AuthService authService = new AuthService();
    private Gson gson = new Gson();
    // Có thể thêm hàm băm để tăng tính bảo mật
    public String processRegisterRest(String jsonBody) {
        try {
            // Nhận dto từ Client qua http
            RegisterRequestDTO dto = gson.fromJson(jsonBody, RegisterRequestDTO.class);
            Response response = authService.register(dto);
            //  Đóng gói kết quả thành JSON và trả về cho ServerApp

            return gson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), null));
        }
    }


    // Handle Login
    public String processLoginRest(String jsonBody) {
        Gson gson = new Gson();
        try {
            LoginRequestDTO loginData = gson.fromJson(jsonBody, LoginRequestDTO.class);

            Response response = authService.login(loginData);
            return gson.toJson(response);
        } catch (Exception e) {
            return gson.toJson(new Response("ERROR", "Lỗi hệ thống: " + e.getMessage(), null));
        }
    }
}