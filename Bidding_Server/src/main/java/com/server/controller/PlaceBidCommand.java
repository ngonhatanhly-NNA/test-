package com.server.controller;

import com.server.exception.AuctionAppException;
import com.server.exception.AuctionException;
import com.server.exception.AuthValidationException;
import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.BidRequestDTO;
import io.javalin.http.Context;

public class PlaceBidCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public PlaceBidCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        BidRequestDTO bidRequest = gson.fromJson(ctx.body(), BidRequestDTO.class);
        if (bidRequest == null) {
            throw new AuthValidationException("Bid request không hợp lệ");
        }

        try {
            AuctionUpdateDTO result = auctionService.placeBid(bidRequest);
            String json = gson.toJson(ResponseUtils.success("Bid placed successfully", result));
            ctx.status(200).result(json).contentType("application/json");
        } catch (AuctionException e) {
            throw AuctionAppException.from(e);
        }
    }
}

