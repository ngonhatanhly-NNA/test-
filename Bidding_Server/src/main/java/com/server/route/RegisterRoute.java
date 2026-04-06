package com.server.route;

import io.javalin.Javalin;
import com.server.controller.AuthController;

public class RegisterRoute {
    public static void RESTregister(Javalin app, AuthController authController){
        // Định tuyến API Đăng ký
        app.post("/api/register", ctx -> {
            String jsonBody = ctx.body(); // Lấy JSON Client gửi
            String jsonResponse = authController.processRegisterRest(jsonBody); // Nhờ AuthService xử lý
            ctx.contentType("application/json");
            ctx.result(jsonResponse); // Trả kết quả về
        });
    }
}
