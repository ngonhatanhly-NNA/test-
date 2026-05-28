package com.server.controller.command;

import com.server.service.AdminService;
import com.shared.network.Response;
import io.javalin.http.Context;

public class AdminGetAllAuctionsCommand extends BaseApiCommand {
    private final AdminService adminService = new AdminService();

    @Override
    public void execute(Context ctx) {
        // Lấy dữ liệu từ service và trả về cho client qua Javalin Context
        Response response = adminService.getAllAuctionsForAdmin();
        ctx.json(response);
    }
}