package com.server.controller;

import com.server.exception.AuctionException;
import com.server.service.AuthService;
import com.google.gson.Gson;
import com.shared.dto.*;
import com.shared.network.Response;
import io.javalin.http.Context;     //Import cái này để dùng Javalin xử lý

// Controller, nơi giao việc cho service, nối DB và (tùy hugnws) quản lí đăng nhập
public class AuthController{

    private final AuthService authService;
    private final Gson gson = new Gson();
    // Có thể thêm hàm băm để tăng tính bảo mật

    public AuthController (AuthService authService){
        this.authService = authService;
    }
    public void processRegisterRest(Context ctx) {
        RegisterRequestDTO dto = gson.fromJson(ctx.body(), RegisterRequestDTO.class);
        authService.register(dto);
        ctx.json(new Response("SUCCESS", "Đăng ký thành công!", null));
    }


    // Handle Login
    // Nhận Context, nhưng trả về Response. Dùng throw để LoginRoute bắt lỗi
    public void processLoginRest(Context ctx) throws Exception {
        LoginRequestDTO loginData = gson.fromJson(ctx.body(), LoginRequestDTO.class);
        // Gọi Service xử lý, nhận về DTO
        UserProfileResponseDTO profile = authService.login(loginData);
        // Lưu session
        ctx.sessionAttribute("username", loginData.getUsername());
        // Controller tự đóng gói Response trả về kèm thông tin User
        ctx.json(new Response("SUCCESS", "Đăng nhập thành công!", profile));
    }

    // Lấy User Profile
    public void getUserProfile(Context ctx) throws Exception {
        String username = ctx.queryParam("username");
        if (username == null || username.trim().isEmpty()) {
            ctx.status(400).json(new Response("ERROR", "Username không được để trống", null));
            return;
        }
        UserProfileResponseDTO profile = authService.getUserProfile(username);
        ctx.json(new Response("SUCCESS", "Tải thông tin người dùng thành công", profile));
    }

    // Cập nhật User Profile
    public void updateProfile(Context ctx) throws Exception {
        BaseProfileUpdateDTO updateData = gson.fromJson(ctx.body(), BaseProfileUpdateDTO.class);
        authService.updateProfile(updateData);
        ctx.json(new Response("SUCCESS", "Cập nhật thông tin thành công", null));
    }
}