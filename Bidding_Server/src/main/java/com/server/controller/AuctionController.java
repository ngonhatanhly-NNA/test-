package com.server.controller;

import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.service.AuctionService;
import com.shared.dto.*;
import com.shared.network.Response;
import io.javalin.http.Context;
import com.google.gson.Gson;

import java.util.List;

// Controller phụ trách chuyển json sang model và đưa ra service để xử lí
public class AuctionController {
    public final AuctionService auctionService;
    private final Gson gson = new Gson();

    public AuctionController(AuctionRepository auctionRepo, BidTransactionRepository bidRepo) {
        this.auctionService = new AuctionService(auctionRepo, bidRepo);
    }

    // các Auction, sản phẩm đang đc đấu giá
    public void getActiveAuctions(Context ctx) {
        try {
            List<AuctionDetailDTO> auctions = auctionService.getActiveAuctions();
            ctx.json(new Response("SUCCESS", "Active auctions loaded", auctions));
        } catch (Exception e) {
            ctx.status(500).json(new Response("FAIL", "Failed to load auctions: " + e.getMessage(), null));
        }
    }

    // Controller của placeBid, chuyển json chứ k phải fnction đâu nha
    public void placeBid(Context ctx) {
        try {
            BidRequestDTO bidRequest = gson.fromJson(ctx.body(), BidRequestDTO.class);
            AuctionUpdateDTO result = auctionService.placeBid(bidRequest);
            ctx.json(new Response("SUCCESS", "Bid placed successfully", result));
        } catch (Exception e) {
            ctx.status(400).json(new Response("FAIL", "Bid failed: " + e.getMessage(), null));
        }
    }
}