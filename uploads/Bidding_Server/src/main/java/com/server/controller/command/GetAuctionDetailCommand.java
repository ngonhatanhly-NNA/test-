package com.server.controller.command;

import com.server.exception.AuctionAppException;
import com.server.exception.AuctionException;
import com.server.exception.AuthValidationException;
import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.shared.dto.AuctionDetailDTO;
import io.javalin.http.Context;

public class GetAuctionDetailCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public GetAuctionDetailCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        long auctionId;
        try {
            auctionId = Long.parseLong(ctx.pathParam("auctionId"));
        } catch (NumberFormatException e) {
            throw new AuthValidationException("ID phiên đấu giá không hợp lệ");
        }

        try {
            AuctionDetailDTO detail = auctionService.getAuctionDetail(auctionId);
            String json = gson.toJson(ResponseUtils.success("Đã tải chi tiết phiên đấu giá", detail));
            ctx.status(200).result(json).contentType("application/json");
        } catch (AuctionException e) {
            throw AuctionAppException.from(e);
        }
    }
}

