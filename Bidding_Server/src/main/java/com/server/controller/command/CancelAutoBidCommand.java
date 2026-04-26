package com.server.controller.command;

import com.server.exception.AuctionAppException;
import com.server.exception.AuctionException;
import com.server.exception.AuthValidationException;
import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.shared.dto.AutoBidCancelDTO;
import io.javalin.http.Context;

public class CancelAutoBidCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public CancelAutoBidCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        long auctionId;
        try {
            auctionId = Long.parseLong(ctx.pathParam("auctionId"));
        } catch (NumberFormatException e) {
            throw new AuthValidationException("ID không hợp lệ");
        }

        AutoBidCancelDTO cancelRequest = gson.fromJson(ctx.body(), AutoBidCancelDTO.class);
        if (cancelRequest == null || cancelRequest.getBidderId() <= 0) {
            throw new AuthValidationException("Bidder ID không hợp lệ");
        }

        try {
            auctionService.cancelAutoBid(auctionId, cancelRequest.getBidderId());
            String json = gson.toJson(ResponseUtils.success("Auto-bid cancelled successfully", null));
            ctx.status(200).result(json).contentType("application/json");
        } catch (AuctionException e) {
            throw AuctionAppException.from(e);
        }
    }
}

