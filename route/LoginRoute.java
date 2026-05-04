package com.server.route;

import com.server.controller.AuthController;
import com.shared.network.Response;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class LoginRoute implements Handler {
    private final AuthController authController;

    public LoginRoute(AuthController authController) {
        this.authController = authController;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        // Giữ nguyên logic try-catch như bạn muốn
        try {
            authController.processLoginRest(ctx);
        } catch (Exception e) {
            ctx.status(401).json(new Response("ERROR", "Lỗi đăng nhập: " + e.getMessage(), null));
        }
    }
}