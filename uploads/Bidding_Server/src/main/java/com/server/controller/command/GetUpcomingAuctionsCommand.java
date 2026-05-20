package com.server.controller.command;

import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import io.javalin.http.Context;

public class GetUpcomingAuctionsCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public GetUpcomingAuctionsCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        String json = gson.toJson(ResponseUtils.success("Lấy danh sách thành công", auctionService.getUpcomingAuctions()));
        ctx.status(200).result(json).contentType("application/json");
    }
}