package com.server.controller;

import com.server.model.Role;
import com.server.security.JwtUtil;
import com.server.service.AuthService;
import com.google.gson.Gson;
import com.shared.dto.*;
import com.shared.network.Response;
import io.javalin.http.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Controller, nơi giao việc cho service, nối DB và (tùy hugnws) quản lí đăng nhập
public class AuthController{
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final Gson gson = new Gson();

    public AuthController (AuthService authService, JwtUtil jwtUtil){
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    public void processRegisterRest(Context ctx) {
        try {
            RegisterRequestDTO dto = gson.fromJson(ctx.body(), RegisterRequestDTO.class);
            authService.register(dto);
            logger.info("Đăng ký thành công: {}", dto.getUsername());
            ctx.json(new Response("SUCCESS", "Đăng ký thành công!", null));
        } catch (Exception e) {
            logger.warn("Đăng ký thất bại: {}", e.getMessage());
            ctx.status(400).json(new Response("ERROR", e.getMessage(), null));
        }
    }

    public void processLoginRest(Context ctx) throws Exception {
        LoginRequestDTO loginData = gson.fromJson(ctx.body(), LoginRequestDTO.class);
        UserProfileResponseDTO profile = authService.login(loginData);
        String token = jwtUtil.generateToken(
                profile.getId(),
                profile.getUsername(),
                Role.valueOf(profile.getRole())
        );
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(profile, token);
        logger.info("Đăng nhập thành công: {} (role: {})", profile.getUsername(), profile.getRole());
        ctx.json(new Response("SUCCESS", "Đăng nhập thành công!", loginResponseDTO));
    }

    public void getUserProfile(Context ctx) throws Exception {
        String authUsername = ctx.attribute("auth.username");
        if (authUsername == null || authUsername.trim().isEmpty()) {
            ctx.status(401).json(new Response("FAIL", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn", null));
            return;
        }

        String requestedUsername = ctx.queryParam("username");
        if (requestedUsername != null && !requestedUsername.trim().isEmpty() && !authUsername.equals(requestedUsername)) {
            logger.warn("User '{}' cố gắng xem profile của '{}'", authUsername, requestedUsername);
            ctx.status(403).json(new Response("FAIL", "Bạn không có quyền xem profile này", null));
            return;
        }

        UserProfileResponseDTO profile = authService.getUserProfile(authUsername);
        ctx.json(new Response("SUCCESS", "Tải thông tin ngườii dùng thành công", profile));
    }

    public void updateProfile(Context ctx) throws Exception {
        BaseProfileUpdateDTO updateData = gson.fromJson(ctx.body(), BaseProfileUpdateDTO.class);
        authService.updateProfile(updateData);
        logger.info("Cập nhật profile thành công cho user id: {}", updateData.getId());
        ctx.json(new Response("SUCCESS", "Cập nhật thông tin thành công", null));
    }
}

