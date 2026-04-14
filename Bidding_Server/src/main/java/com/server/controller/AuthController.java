package com.server.controller;

import com.server.service.AuthService;
import com.google.gson.Gson;
import com.shared.dto.*;
import com.shared.network.Response;

// Controller, nơi giao việc cho service, nối DB và (tùy hugnws) quản lí đăng nhập
public class AuthController{

    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();
    // Có thể thêm hàm băm để tăng tính bảo mật
    public Response processRegisterRest(String jsonBody) {
        RegisterRequestDTO dto = gson.fromJson(jsonBody, RegisterRequestDTO.class);
        return authService.register(dto);
    }


    // Handle Login
    public Response processLoginRest(String jsonBody) {
        LoginRequestDTO loginData = gson.fromJson(jsonBody, LoginRequestDTO.class);
        return authService.login(loginData);
    }
}