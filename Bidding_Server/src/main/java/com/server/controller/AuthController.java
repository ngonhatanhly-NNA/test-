package com.server.controller;

import com.server.model.Role;
import com.server.security.JwtUtil;
import com.server.service.AuthService;
import com.google.gson.Gson;
import com.shared.dto.*;
import com.shared.network.Response;
import io.javalin.http.Context;     //Import cái này để dùng Javalin xử lý

// Controller, nơi giao việc cho service, nối DB và (tùy hugnws) quản lí đăng nhập
public class AuthController{

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final Gson gson = new Gson();
    // Có thể thêm hàm băm để tăng tính bảo mật

    public AuthController (AuthService authService, JwtUtil jwtUtil){
        this.authService = authService;
        this.jwtUtil = jwtUtil;
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
        String token = jwtUtil.generateToken(
                profile.getId(),
                profile.getUsername(),
                Role.valueOf(profile.getRole())
        );
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(profile, token);
        // Controller tự đóng gói Response trả về kèm thông tin User
        ctx.json(new Response("SUCCESS", "Đăng nhập thành công!", loginResponseDTO));
    }

    // Lấy User Profile
    public void getUserProfile(Context ctx) throws Exception {
        String authUsername = ctx.attribute("auth.username");
        if (authUsername == null || authUsername.trim().isEmpty()) {
            ctx.status(401).json(new Response("FAIL", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn", null));
            return;
        }

        String requestedUsername = ctx.queryParam("username");
        if (requestedUsername != null && !requestedUsername.trim().isEmpty() && !authUsername.equals(requestedUsername)) {
            ctx.status(403).json(new Response("FAIL", "Bạn không có quyền xem profile này", null));
            return;
        }

        UserProfileResponseDTO profile = authService.getUserProfile(authUsername);
        ctx.json(new Response("SUCCESS", "Tải thông tin người dùng thành công", profile));
    }

    // Cập nhật User Profile
    public void updateProfile(Context ctx) throws Exception {
        BaseProfileUpdateDTO updateData = gson.fromJson(ctx.body(), BaseProfileUpdateDTO.class);
        authService.updateProfile(updateData);
        ctx.json(new Response("SUCCESS", "Cập nhật thông tin thành công", null));
    }
}