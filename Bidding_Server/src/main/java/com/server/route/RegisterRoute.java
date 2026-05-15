package com.server.route;

import com.server.controller.AuthController;
import com.shared.network.Response;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class RegisterRoute implements Handler {
    private final AuthController authController;

    public RegisterRoute(AuthController authController) {
        this.authController = authController;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            authController.processRegisterRest(ctx);
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi đăng ký: " + e.getMessage(), null));
        }
    }
}