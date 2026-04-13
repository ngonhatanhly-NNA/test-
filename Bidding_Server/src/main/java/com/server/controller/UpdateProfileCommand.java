package com.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.server.model.Role;
import com.server.service.UserService;
import com.shared.dto.AdminProfileUpdateDTO;
import com.shared.dto.BaseProfileUpdateDTO;
import com.shared.dto.BidderProfileUpdateDTO;
import com.shared.dto.SellerProfileUpdateDTO;
import com.shared.network.Response;
import io.javalin.http.Context;

public class UpdateProfileCommand extends BaseApiCommand {

    private final UserService userService;
    private final Gson gson = new Gson();

    public UpdateProfileCommand(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        // 1. Giả sử ta lấy được Username từ Session/Token (Vì em hardcode "admin_team13" ở Client nên thầy tạm mock ở đây)
        // TODO: Đổi thành ctx.sessionAttribute("username") khi ráp luồng Đăng nhập xong
        String currentUsername = "admin_team13";

        // 2. Lấy chuỗi JSON từ Client
        String jsonBody = ctx.body();

        // 3. HỎI DATABASE: "Ê, thằng này đang là Role gì?"
        Role currentRole = userService.getUserRole(currentUsername);

        if (currentRole == null) {
            ctx.status(404).json(new Response("FAIL", "Không tìm thấy người dùng", null));
            return;
        }

        // 4. ÉP KIỂU ĐỘNG: Tùy theo Role mà dùng cái khuôn DTO nào
        BaseProfileUpdateDTO updateDTO = null;

        switch (currentRole) {
            case ADMIN:
                updateDTO = gson.fromJson(jsonBody, AdminProfileUpdateDTO.class);
                break;
            case SELLER:
                updateDTO = gson.fromJson(jsonBody, SellerProfileUpdateDTO.class);
                break;
            case BIDDER:
            default:
                updateDTO = gson.fromJson(jsonBody, BidderProfileUpdateDTO.class);
                break;
        }

        // 5. Ném cho Service xử lý (Service của Lead đã xịn sẵn rồi)
        Response response = userService.updateProfile(currentUsername, updateDTO);

        // 6. Trả kết quả (Dùng thẳng ctx.json vì ta đã gỡ Jackson hôm qua)
        if ("SUCCESS".equals(response.getStatus())) {
            ctx.status(200).json(response);
        } else {
            ctx.status(400).json(response); // Lỗi logic
        }
    }
}