package com.server.route;

import com.server.controller.AuthController;
import com.google.gson.Gson;
import com.server.exception.AppException;
import com.server.util.ResponseUtils;
import com.shared.network.Response;
import io.javalin.Javalin;

public class LoginRoute {
    public static void RESTLogin (Javalin app, AuthController authController){
        Gson gson = new Gson();
        // API Đăng nhập:
        app.post("/api/login", ctx -> {
            try {
                String jsonBody = ctx.body();
                Response response = authController.processLoginRest(jsonBody);
                ctx.status(200).contentType("application/json").result(gson.toJson(response));
            } catch (AppException e) {
                ctx.status(e.getHttpStatus()).contentType("application/json")
                        .result(gson.toJson(ResponseUtils.fromAppException(e)));
            } catch (IllegalArgumentException e) {
                ctx.status(400).contentType("application/json")
                        .result(gson.toJson(ResponseUtils.fail("VALIDATION", e.getMessage())));
            } catch (Exception e) {
                ctx.status(500).contentType("application/json")
                        .result(gson.toJson(ResponseUtils.internalError("Lỗi máy chủ.")));
            }
        });
    }
}
