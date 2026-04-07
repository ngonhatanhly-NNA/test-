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

    // Giữ Constructor cho Unit Test
    public AuctionController(AuctionRepository auctionRepo, BidTransactionRepository bidRepo) {
        this.auctionService = new AuctionService(auctionRepo, bidRepo);
    }

    // Lấy các sản phẩm đang được đấu giá
    public void getActiveAuctions(Context ctx) {
        try {
            List<AuctionDetailDTO> auctions = auctionService.getActiveAuctions();
            ctx.json(new Response("SUCCESS", "Active auctions loaded", auctions));
        } catch (Exception e) {
            ctx.status(500).json(new Response("FAIL", "Failed to load auctions: " + e.getMessage(), null));
        }
    }

    // Controller của placeBid, chuyển json thành model
    public void placeBid(Context ctx) {
        try {
            BidRequestDTO bidRequest = gson.fromJson(ctx.body(), BidRequestDTO.class);
            AuctionUpdateDTO result = auctionService.placeBid(bidRequest);
            ctx.json(new Response("SUCCESS", "Bid placed successfully", result));
        } catch (Exception e) {
            ctx.status(400).json(new Response("FAIL", "Bid failed: " + e.getMessage(), null));
        }
    }

   // Lay detail
    public void getAuctionDetail(Context ctx) {
        try {
            // Lấy ID từ đường dẫn URL (VD: /api/auctions/5 -> Lấy số 5)
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));

            // TODO: getAuctionById trong AuctionService sau
            // AuctionDetailDTO detail = auctionService.getAuctionById(auctionId);
            // ctx.json(new Response("SUCCESS", "Get detail successfully", detail));

            ctx.json(new Response("SUCCESS", "Đã kết nối API lấy chi tiết số " + auctionId + " (Chưa có logic xử lý)", null));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("FAIL", "ID phiên đấu giá không hợp lệ!", null));
        } catch (Exception e) {
            ctx.status(500).json(new Response("FAIL", "Lỗi server: " + e.getMessage(), null));
        }
    }
}