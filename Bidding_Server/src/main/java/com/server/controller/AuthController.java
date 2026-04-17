package com.server.controller;

import com.server.service.AuthService;
import com.google.gson.Gson;
import com.shared.dto.*;
import com.shared.network.Response;
import io.javalin.http.Context;     //Import cái này để dùng Javalin xử lý

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
    // Nhận Context, nhưng trả về Response. Dùng throw để LoginRoute bắt lỗi
    public Response processLoginRest(Context ctx) throws Exception {
        // 1. Lấy JSON từ ctx.body()
        LoginRequestDTO loginData = gson.fromJson(ctx.body(), LoginRequestDTO.class);

        // 2. Gọi Service xử lý đăng nhập
        Response response = authService.login(loginData);

        // 3. NẾU THÀNH CÔNG -> CẤP THẺ BÀI (SESSION) LÊN SERVER
        if ("SUCCESS".equals(response.getStatus())) {
            ctx.sessionAttribute("username", loginData.getUsername());
        }

        // 4. Trả Response về cho LoginRoute để nó In ra màn hình hoặc bắt lỗi
        return response;
    }
}