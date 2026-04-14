package com.server.route;

import io.javalin.Javalin;
import com.server.controller.AuthController;
import com.google.gson.Gson;
import com.server.exception.AppException;
import com.server.util.ResponseUtils;
import com.shared.network.Response;

public class RegisterRoute {
    public static void RESTregister(Javalin app, AuthController authController){
        Gson gson = new Gson();
        // Định tuyến API Đăng ký
        app.post("/api/register", ctx -> {
            try {
                String jsonBody = ctx.body(); // Lấy JSON Client gửi
                Response response = authController.processRegisterRest(jsonBody); // Nhờ AuthService xử lý
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
