package com.server.controller;

import com.server.exception.AuctionAppException;
import com.server.exception.AuctionException;
import com.server.exception.AuthValidationException;
import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.shared.dto.AutoBidUpdateDTO;
import io.javalin.http.Context;

public class UpdateAutoBidAmountCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public UpdateAutoBidAmountCommand(AuctionService auctionService) {
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

        AutoBidUpdateDTO updateRequest = gson.fromJson(ctx.body(), AutoBidUpdateDTO.class);
        if (updateRequest == null || updateRequest.getBidderId() <= 0 || updateRequest.getMaxBidAmount() == null) {
            throw new AuthValidationException("Request không hợp lệ");
        }

        try {
            auctionService.updateAutoBidAmount(auctionId, updateRequest.getBidderId(), updateRequest.getMaxBidAmount());
            String json = gson.toJson(ResponseUtils.success("Auto-bid updated successfully", null));
            ctx.status(200).result(json).contentType("application/json");
        } catch (AuctionException e) {
            throw AuctionAppException.from(e);
        }
    }
}

