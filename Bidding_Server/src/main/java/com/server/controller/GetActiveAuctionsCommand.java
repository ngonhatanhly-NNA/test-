package com.server.controller;

import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.shared.dto.AuctionDetailDTO;
import io.javalin.http.Context;

import java.util.List;

public class GetActiveAuctionsCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public GetActiveAuctionsCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    protected void execute(Context ctx) {
        List<AuctionDetailDTO> auctions = auctionService.getActiveAuctions();
        String json = gson.toJson(ResponseUtils.success("Active auctions loaded", auctions));
        ctx.status(200).result(json).contentType("application/json");
    }
}

