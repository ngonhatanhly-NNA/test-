package com.server.controller.command;

import com.server.service.AdminService;
import com.shared.network.Response;
import io.javalin.http.Context;

public class AdminGetDetailedHistoryCommand extends BaseApiCommand {
    private final AdminService adminService = new AdminService();

    @Override
    public void execute(Context ctx) {
        try {
            // Lấy ID từ URL (Ví dụ: /api/admin/auctions/5/history)
            long auctionId = Long.parseLong(ctx.pathParam("id"));
            Response response = adminService.getDetailedAuctionHistory(auctionId);
            ctx.json(response);
        } catch (Exception e) {
            ctx.json(new Response("ERROR", "ID phiên đấu giá không hợp lệ", null));
        }
    }
}