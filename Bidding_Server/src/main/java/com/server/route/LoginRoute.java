package com.server.route;

import com.server.controller.AuthController;
import io.javalin.Javalin;

public class LoginRoute {
    public static void RESTLogin (Javalin app, AuthController authController){
        // API Đăng nhập:
        app.post("/api/login", ctx -> {
            String jsonBody = ctx.body();
            String jsonResponse = authController.processLoginRest(jsonBody);
            ctx.contentType("application/json");
            ctx.result(jsonResponse);
        });
    }
}
